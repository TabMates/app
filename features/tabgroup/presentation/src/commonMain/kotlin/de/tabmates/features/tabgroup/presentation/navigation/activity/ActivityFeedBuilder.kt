package de.tabmates.features.tabgroup.presentation.navigation.activity

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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Turns a merged feed into the date-bucketed rows the UI renders.
 *
 * Shared by the account-wide Activity tab and the group History tab so the two can't drift. It
 * formats *values* only — amounts, dates and user names — and leaves every label (the action verb,
 * the field names, "You") to the composable, because `getString` cannot be called from a ViewModel.
 */
internal object ActivityFeedBuilder {
    private const val SEPARATOR = " · "
    private const val SELF_SEED = "self"
    private const val FALLBACK_NAME = "Someone"
    private const val DEFAULT_DECIMALS = 2

    fun build(
        items: List<ActivityFeedItem>,
        currentUserId: String,
        groupTitles: Map<String, String>,
        participantNames: Map<String, String>,
        currencyByCode: Map<String, Currency>,
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
                monthNames = monthNames,
                now = now,
                includeGroupName = includeGroupName,
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

    private class RenderContext(
        val currentUserId: String,
        val groupTitles: Map<String, String>,
        val participantNames: Map<String, String>,
        val currencyByCode: Map<String, Currency>,
        val monthNames: List<String>,
        val now: Instant,
        val includeGroupName: Boolean,
    ) {
        fun render(event: ActivityEvent): ActivityItem {
            val groupTitle = groupTitles[event.groupId]
            val actorName = nameOf(event.actorUserId)
            val targetName = event.targetUsername ?: event.targetUserId?.let { nameOf(it) } ?: FALLBACK_NAME
            val kind =
                when (event.type) {
                    ActivityEventType.ENTRY_CREATED -> ActivityKind.EntryAdded(event.entryTitle.orEmpty())
                    ActivityEventType.ENTRY_UPDATED -> ActivityKind.EntryEdited(event.entryTitle.orEmpty())
                    ActivityEventType.ENTRY_DELETED -> ActivityKind.EntryDeleted(event.entryTitle.orEmpty())
                    ActivityEventType.GROUP_CREATED -> ActivityKind.GroupCreated(groupTitle.orEmpty())
                    ActivityEventType.GROUP_UPDATED -> ActivityKind.GroupUpdated(groupTitle.orEmpty())
                    ActivityEventType.MEMBER_JOINED -> ActivityKind.MemberJoined(targetName)
                    ActivityEventType.MEMBER_LEFT -> ActivityKind.MemberLeft(targetName)
                    ActivityEventType.MEMBER_REMOVED -> ActivityKind.MemberRemoved(targetName)
                    ActivityEventType.UNKNOWN -> ActivityKind.Unknown
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
                        instant = event.occurredAt,
                    ),
                diffs = event.changes.map { diff(it.field, it.oldValue, it.newValue, event.currencyCode) },
                isDeleted = event.type == ActivityEventType.ENTRY_DELETED,
                clickTarget = clickTarget(event.type, event.groupId, event.tabEntryId, event.entryType),
            )
        }

        fun render(pending: ActivityFeedItem.Pending): ActivityItem {
            val groupTitle = pending.groupId?.let { groupTitles[it] }
            val title = pending.entryTitle.orEmpty()
            val kind =
                when (pending.type) {
                    ActivityEventType.ENTRY_UPDATED -> ActivityKind.EntryEdited(title)
                    ActivityEventType.ENTRY_DELETED -> ActivityKind.EntryDeleted(title)
                    else -> ActivityKind.EntryAdded(title)
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
                        instant = pending.occurredAt,
                    ),
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
            diffs: List<ActivityDiff> = emptyList(),
            isPending: Boolean = false,
            isDeleted: Boolean = false,
            clickTarget: ActivityClickTarget,
        ): ActivityItem {
            val isYou = actorId == currentUserId
            return ActivityItem(
                id = id,
                initials = actorName.take(2).uppercase(),
                colorSeed = if (isYou) SELF_SEED else actorId,
                actor = actorName,
                actorIsYou = isYou,
                kind = kind,
                subtitle = subtitle,
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
                // The entry is gone; opening its detail would show an empty shell.
                type == ActivityEventType.ENTRY_DELETED -> {
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
            return formatAmount(amount, symbol, currency?.decimalDigits ?: DEFAULT_DECIMALS)
        }

        private fun subtitle(
            groupTitle: String?,
            amountText: String?,
            instant: Instant,
        ): String =
            listOfNotNull(
                groupTitle.takeIf { includeGroupName },
                amountText,
                relativeTime(instant, now),
            ).joinToString(SEPARATOR)

        fun nameOf(userId: String): String = participantNames[userId] ?: FALLBACK_NAME
    }

    private fun relativeTime(
        from: Instant,
        now: Instant,
    ): String {
        val elapsed = now - from
        return when {
            elapsed < 1.minutes -> "now"
            elapsed < 1.hours -> "${elapsed.inWholeMinutes}m"
            elapsed < 1.days -> "${elapsed.inWholeHours}h"
            else -> "${elapsed.inWholeDays}d"
        }
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
