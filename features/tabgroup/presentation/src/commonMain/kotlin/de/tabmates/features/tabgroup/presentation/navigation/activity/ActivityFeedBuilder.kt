package de.tabmates.features.tabgroup.presentation.navigation.activity

import de.tabmates.core.presentation.format.DEFAULT_CURRENCY_DECIMALS
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.core.presentation.util.RelativeTimeSpan
import de.tabmates.core.presentation.util.relativeTimeSpan
import de.tabmates.features.tabgroup.domain.activity.ActivityEntryType
import de.tabmates.features.tabgroup.domain.activity.ActivityEvent
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityField
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.formatAmount
import de.tabmates.features.tabgroup.presentation.util.platformShortMonthNames
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Turns a merged feed into the date-bucketed rows the UI renders.
 *
 * Shared by the account-wide Activity tab and the group History tab so the two can't drift. It
 * formats *values* only — amounts, dates and user names — and leaves every label (the sentence, the
 * field names, "You", the timestamp) to the composable, because `getString` cannot be called from a
 * ViewModel. A name it cannot resolve comes out blank rather than as an English placeholder.
 */
internal object ActivityFeedBuilder {
    internal const val SEPARATOR = " · "
    private const val SELF_SEED = "self"
    private const val UNKNOWN_INITIALS = "?"

    fun build(
        items: List<ActivityFeedItem>,
        currentUserId: String,
        groupTitles: Map<String, String>,
        participantNames: Map<String, String>,
        currencyByCode: Map<String, Currency>,
        numberSymbols: NumberSymbols,
        now: Instant,
        includeGroupName: Boolean = true,
    ): List<ActivitySection> {
        val monthNames = platformShortMonthNames()
        val context =
            RenderContext(
                currentUserId = currentUserId,
                groupTitles = groupTitles,
                participantNames = participantNames,
                currencyByCode = currencyByCode,
                numberSymbols = numberSymbols,
                monthNames = monthNames,
                now = now,
                includeGroupName = includeGroupName,
                deletedEntryIds = deletedEntryIds(items),
            )

        // Keyed rather than sequential: pending rows sit on top unconditionally, so a pending write
        // that is older than a freshly received server event would otherwise open a second section
        // with the same header.
        val byBucket = linkedMapOf<ActivityBucket, MutableList<ActivityItem>>()
        items.forEach { feedItem ->
            val item =
                when (feedItem) {
                    is ActivityFeedItem.Persisted -> context.render(feedItem.event)
                    is ActivityFeedItem.Pending -> context.render(feedItem)
                }
            val bucket = bucketFor(feedItem.occurredAt, now)
            byBucket.getOrPut(bucket) { mutableListOf() } += item
        }
        return byBucket.map { (bucket, bucketItems) -> ActivitySection(bucket, bucketItems) }
    }

    /**
     * The entries this feed reports as gone, so their earlier "added"/"edited" rows stop linking to
     * a detail screen that no longer has anything to show.
     *
     * Scanning the loaded window is enough: a delete always carries a higher `seq` than the create
     * and edits it follows, and the feed is the newest rows by `seq`, so a create can never be in
     * the window without its delete. Pending outbox rows are exempt from the window's limit
     * entirely, which covers a delete that has not reached the server yet.
     */
    private fun deletedEntryIds(items: List<ActivityFeedItem>): Set<String> =
        items.mapNotNullTo(mutableSetOf()) { feedItem ->
            if (feedItem.type != ActivityEventType.ENTRY_DELETED) return@mapNotNullTo null
            when (feedItem) {
                is ActivityFeedItem.Persisted -> feedItem.event.tabEntryId
                is ActivityFeedItem.Pending -> feedItem.tabEntryId
            }
        }

    private class RenderContext(
        val currentUserId: String,
        val groupTitles: Map<String, String>,
        val participantNames: Map<String, String>,
        val currencyByCode: Map<String, Currency>,
        val numberSymbols: NumberSymbols,
        val monthNames: List<String>,
        val now: Instant,
        val includeGroupName: Boolean,
        val deletedEntryIds: Set<String>,
    ) {
        fun render(event: ActivityEvent): ActivityItem {
            val groupTitle = groupTitles[event.groupId]
            val actorName = nameOf(event.actorUserId)
            val targetName = event.targetUsername ?: event.targetUserId?.let { nameOf(it) } ?: ""
            val entryTitle = event.entryTitle.orEmpty()
            val isSettlement = event.entryType == ActivityEntryType.SETTLEMENT
            val isSelf = event.isSelfTargeted()
            val kind =
                when (event.type) {
                    ActivityEventType.ENTRY_CREATED -> {
                        if (isSettlement) ActivityKind.SettlementAdded else ActivityKind.EntryAdded(entryTitle)
                    }

                    ActivityEventType.ENTRY_UPDATED -> {
                        if (isSettlement) ActivityKind.SettlementEdited else ActivityKind.EntryEdited(entryTitle)
                    }

                    ActivityEventType.ENTRY_DELETED -> {
                        if (isSettlement) ActivityKind.SettlementDeleted else ActivityKind.EntryDeleted(entryTitle)
                    }

                    ActivityEventType.GROUP_CREATED -> {
                        ActivityKind.GroupCreated(groupTitle.orEmpty())
                    }

                    ActivityEventType.GROUP_UPDATED -> {
                        ActivityKind.GroupUpdated(groupTitle.orEmpty())
                    }

                    // The server names the actor as the target of a self-join, and someone else
                    // whenever that person was invited or added as a placeholder instead.
                    ActivityEventType.MEMBER_JOINED -> {
                        if (isSelf) ActivityKind.MemberJoined else ActivityKind.MemberAdded(targetName)
                    }

                    ActivityEventType.MEMBER_LEFT -> {
                        ActivityKind.MemberLeft
                    }

                    ActivityEventType.MEMBER_REMOVED -> {
                        if (isSelf) ActivityKind.MemberLeft else ActivityKind.MemberRemoved(targetName)
                    }

                    ActivityEventType.UNKNOWN -> {
                        ActivityKind.Unknown
                    }
                }
            return item(
                id = event.id,
                actorId = event.actorUserId,
                actorName = actorName,
                kind = kind,
                subtitle =
                    subtitle(
                        groupTitle = groupTitle,
                        amountText = amountText(event.amount, event.currencyCode),
                    ),
                occurredAgo = relativeTimeSpan(from = event.occurredAt, now = now),
                diffs = event.changes.map { diff(it.field, it.oldValue, it.newValue, event.currencyCode) },
                isDeleted = event.type == ActivityEventType.ENTRY_DELETED,
                clickTarget = clickTarget(event.type, event.groupId, event.tabEntryId, event.entryType),
            )
        }

        fun render(pending: ActivityFeedItem.Pending): ActivityItem {
            val groupTitle = pending.groupId?.let { groupTitles[it] }
            val title = pending.entryTitle.orEmpty()
            val isSettlement = pending.entryType == ActivityEntryType.SETTLEMENT
            val kind =
                when (pending.type) {
                    ActivityEventType.ENTRY_UPDATED -> {
                        if (isSettlement) ActivityKind.SettlementEdited else ActivityKind.EntryEdited(title)
                    }

                    ActivityEventType.ENTRY_DELETED -> {
                        if (isSettlement) ActivityKind.SettlementDeleted else ActivityKind.EntryDeleted(title)
                    }

                    else -> {
                        if (isSettlement) ActivityKind.SettlementAdded else ActivityKind.EntryAdded(title)
                    }
                }
            return item(
                // Namespaced so a pending row and the server row that replaces it can never collide
                // as LazyColumn keys during the hand-off.
                id = "pending-${pending.outboxId}",
                actorId = currentUserId,
                actorName = nameOf(currentUserId),
                kind = kind,
                subtitle =
                    subtitle(
                        groupTitle = groupTitle,
                        amountText = amountText(pending.amount, pending.currencyCode),
                    ),
                occurredAgo = relativeTimeSpan(from = pending.occurredAt, now = now),
                isPending = true,
                isDeleted = pending.type == ActivityEventType.ENTRY_DELETED,
                clickTarget =
                    clickTarget(pending.type, pending.groupId, pending.tabEntryId, pending.entryType),
            )
        }

        private fun item(
            id: String,
            actorId: String,
            actorName: String,
            kind: ActivityKind,
            subtitle: String,
            occurredAgo: RelativeTimeSpan,
            diffs: List<ActivityDiff> = emptyList(),
            isPending: Boolean = false,
            isDeleted: Boolean = false,
            clickTarget: ActivityClickTarget,
        ): ActivityItem {
            val isYou = actorId == currentUserId
            return ActivityItem(
                id = id,
                initials = actorName.take(2).uppercase().ifBlank { UNKNOWN_INITIALS },
                colorSeed = if (isYou) SELF_SEED else actorId,
                actor = actorName,
                actorIsYou = isYou,
                kind = kind,
                subtitle = subtitle,
                occurredAgo = occurredAgo,
                diffs = diffs,
                isPending = isPending,
                isDeleted = isDeleted,
                clickTarget = clickTarget,
            )
        }

        private fun clickTarget(
            type: ActivityEventType,
            groupId: String?,
            tabEntryId: String?,
            entryType: ActivityEntryType?,
        ): ActivityClickTarget =
            when {
                // The entry is gone; opening its detail would show an empty shell. This covers the
                // deletion row itself *and* the entry's earlier add/edit rows, which outlive it.
                type == ActivityEventType.ENTRY_DELETED ||
                    (tabEntryId != null && tabEntryId in deletedEntryIds) -> {
                    ActivityClickTarget.None
                }

                type.isEntryEvent -> {
                    if (tabEntryId != null && groupId != null) {
                        ActivityClickTarget.Entry(
                            tabEntryId = tabEntryId,
                            groupId = groupId,
                            isSettlement = entryType == ActivityEntryType.SETTLEMENT,
                        )
                    } else {
                        ActivityClickTarget.None
                    }
                }

                type == ActivityEventType.UNKNOWN -> {
                    ActivityClickTarget.None
                }

                groupId != null -> {
                    ActivityClickTarget.Group(groupId)
                }

                else -> {
                    ActivityClickTarget.None
                }
            }

        private fun diff(
            field: ActivityField,
            oldValue: String?,
            newValue: String?,
            currencyCode: String?,
        ): ActivityDiff =
            ActivityDiff(
                field = field,
                oldValue = formatValue(field, oldValue, currencyCode),
                newValue = formatValue(field, newValue, currencyCode),
            )

        private fun formatValue(
            field: ActivityField,
            raw: String?,
            currencyCode: String?,
        ): String? {
            if (raw.isNullOrBlank()) return null
            return when (field) {
                ActivityField.AMOUNT -> raw.toDoubleOrNull()?.let { amountText(it, currencyCode) } ?: raw

                ActivityField.ENTRY_DATE -> formatDate(raw)

                // Blank, not null, when the id resolves to nobody: the composable tells the two
                // apart — blank names an unknown person, null means the diff has no such side.
                ActivityField.PAID_BY, ActivityField.RECEIVED_BY -> nameOf(raw)

                // A flag, not a diff — the composable renders a single "split changed" line.
                ActivityField.SPLITS -> null

                else -> raw
            }
        }

        private fun formatDate(raw: String): String {
            val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return raw
            val month = monthNames.getOrNull(date.month.number - 1) ?: return raw
            return "$month ${date.day}"
        }

        private fun amountText(
            amount: Double?,
            currencyCode: String?,
        ): String? {
            if (amount == null) return null
            val currency = currencyCode?.let { currencyByCode[it] }
            val symbol = currency?.nativeSymbol ?: currencyCode.orEmpty()
            return formatAmount(
                amount,
                symbol,
                currency?.decimalDigits ?: DEFAULT_CURRENCY_DECIMALS,
                numberSymbols,
            )
        }

        private fun subtitle(
            groupTitle: String?,
            amountText: String?,
        ): String =
            listOfNotNull(
                groupTitle.takeIf { includeGroupName },
                amountText,
            ).joinToString(SEPARATOR)

        /**
         * True when the event describes the actor themselves. A target the event does not identify
         * counts as the actor, so a row can never print the same person twice.
         */
        private fun ActivityEvent.isSelfTargeted(): Boolean = targetUserId?.let { it == actorUserId } ?: true

        /** Blank for a participant this device does not know — the composable names them. */
        fun nameOf(userId: String): String = participantNames[userId].orEmpty()
    }

    private fun bucketFor(
        instant: Instant,
        now: Instant,
    ): ActivityBucket =
        when (val key = instant.dateKey()) {
            now.dateKey() -> {
                ActivityBucket.Today
            }

            (now - 1.days).dateKey() -> {
                ActivityBucket.Yesterday
            }

            else -> {
                val pieces = key.split("-")
                val month = pieces.getOrNull(1)?.toIntOrNull() ?: 1
                val day = pieces.getOrNull(2)?.toIntOrNull() ?: 1
                ActivityBucket.OnDate(monthIndex = (month - 1).coerceIn(0, 11), day = day)
            }
        }

    private fun Instant.dateKey(): String = toString().substringBefore('T')
}
