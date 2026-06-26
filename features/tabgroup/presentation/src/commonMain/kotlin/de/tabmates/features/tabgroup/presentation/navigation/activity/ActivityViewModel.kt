package de.tabmates.features.tabgroup.presentation.navigation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.formatAmount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class ActivityViewModel(
    groupRepository: GroupRepository,
    tabEntryRepository: TabEntryRepository,
    currencyRepository: CurrencyRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val currentUserId =
        sessionStorage
            .get()
            ?.user
            ?.id
            .orEmpty()

    val state: StateFlow<ActivityState> =
        combine(
            groupRepository.getGroups(),
            currencyRepository.getCurrencies(),
        ) { groups, currencies -> groups to currencies }
            .flatMapLatest { (groups, currencies) ->
                if (groups.isEmpty()) {
                    flowOf(ActivityState(isLoading = false))
                } else {
                    combine(
                        groups.map { group ->
                            tabEntryRepository
                                .getTabEntriesForGroup(group.id)
                                .map { entries -> group to entries }
                        },
                    ) { groupEntries -> buildState(groupEntries.toList(), currencies) }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = ActivityState(),
            )

    private fun buildState(
        groupEntries: List<Pair<Group, List<TabEntry>>>,
        currencies: List<Currency>,
    ): ActivityState {
        val currencyByCode = currencies.associateBy { it.code }
        val now = Clock.System.now()
        val raw = mutableListOf<Pair<Instant, ActivityItem>>()

        groupEntries.forEach { (group, entries) ->
            val namesById = group.participants.associate { it.userId to it.username }

            fun name(userId: String) = namesById[userId] ?: FALLBACK_NAME

            // Group created.
            raw +=
                group.createdAt to
                item(
                    id = "group-${group.id}",
                    groupId = group.id,
                    actorId = group.creator.userId,
                    actorName = name(group.creator.userId),
                    kind = ActivityKind.Created(group.title),
                    subtitle = relativeTime(group.createdAt, now),
                )

            entries.forEach { entry ->
                val symbol = currencyByCode[entry.currencyCode]?.nativeSymbol ?: entry.currencyCode
                val decimals = currencyByCode[entry.currencyCode]?.decimalDigits ?: DEFAULT_DECIMALS
                val amountText = formatAmount(entry.amount, symbol, decimals)
                if (entry.isDeleted) {
                    val actorId = entry.deletedByUserId ?: entry.creatorId
                    raw +=
                        (entry.deletedAt ?: entry.lastModifiedAt) to
                        item(
                            id = "del-${entry.tabEntryId}",
                            groupId = group.id,
                            actorId = actorId,
                            actorName = name(actorId),
                            kind = ActivityKind.Deleted(entry.title),
                            subtitle =
                                subtitle(
                                    group.title,
                                    amountText,
                                    entry.deletedAt ?: entry.lastModifiedAt,
                                    now,
                                ),
                            isDeleted = true,
                        )
                    return@forEach
                }
                val event = entryEvent(entry, ::name)
                raw +=
                    event.time to
                    item(
                        id = entry.tabEntryId,
                        groupId = group.id,
                        actorId = event.actorId,
                        actorName = name(event.actorId),
                        kind = event.kind,
                        subtitle = subtitle(group.title, amountText, event.time, now),
                    )
            }
        }

        val sorted = raw.sortedByDescending { it.first }
        val sections = mutableListOf<ActivitySection>()
        var currentBucket: ActivityBucket? = null
        var currentItems = mutableListOf<ActivityItem>()
        sorted.forEach { (instant, activityItem) ->
            val bucket = bucketFor(instant, now)
            if (bucket != currentBucket) {
                if (currentBucket != null) sections += ActivitySection(currentBucket, currentItems)
                currentBucket = bucket
                currentItems = mutableListOf()
            }
            currentItems += activityItem
        }
        if (currentBucket != null) sections += ActivitySection(currentBucket, currentItems)

        return ActivityState(isLoading = false, sections = sections)
    }

    private data class EntryEvent(
        val kind: ActivityKind,
        val actorId: String,
        val time: Instant,
    )

    private fun entryEvent(
        entry: TabEntry,
        name: (String) -> String,
    ): EntryEvent =
        when (entry) {
            is TabEntry.Settlement -> {
                EntryEvent(
                    kind = settlementKind(entry, name),
                    actorId = entry.paidByUserId,
                    time = entry.lastModifiedAt,
                )
            }

            is TabEntry.Expense -> {
                splitEntryEvent(entry, entry.title)
            }

            is TabEntry.Income -> {
                splitEntryEvent(entry, entry.title)
            }
        }

    // An entry whose server version has advanced past 0 has been edited since it was added.
    private fun splitEntryEvent(
        entry: TabEntry,
        title: String,
    ): EntryEvent =
        if (entry.version > 0) {
            EntryEvent(ActivityKind.Edited(title), entry.lastModifiedByUserId, entry.lastModifiedAt)
        } else {
            EntryEvent(ActivityKind.Added(title), entry.creatorId, entry.lastModifiedAt)
        }

    private fun settlementKind(
        entry: TabEntry.Settlement,
        name: (String) -> String,
    ): ActivityKind =
        if (entry.receivedByUserId == currentUserId && entry.paidByUserId != currentUserId) {
            ActivityKind.SettledWithYou
        } else {
            ActivityKind.SettledWith(name(entry.receivedByUserId))
        }

    private fun item(
        id: String,
        groupId: String,
        actorId: String,
        actorName: String,
        kind: ActivityKind,
        subtitle: String,
        isDeleted: Boolean = false,
    ): ActivityItem {
        val isYou = actorId == currentUserId
        return ActivityItem(
            id = id,
            groupId = groupId,
            initials = actorName.take(2).uppercase(),
            colorSeed = if (isYou) SELF_SEED else actorId,
            actor = actorName,
            actorIsYou = isYou,
            kind = kind,
            subtitle = subtitle,
            isDeleted = isDeleted,
        )
    }

    private fun subtitle(
        groupTitle: String,
        amountText: String,
        instant: Instant,
        now: Instant,
    ): String = listOf(groupTitle, amountText, relativeTime(instant, now)).joinToString(SEPARATOR)

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
    ): ActivityBucket {
        return when (val key = instant.dateKey()) {
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
    }

    private fun Instant.dateKey(): String = toString().substringBefore('T')

    private companion object {
        private const val DEFAULT_DECIMALS = 2
        private const val SEPARATOR = " · "
        private const val SELF_SEED = "self"
        private const val FALLBACK_NAME = "Someone"
    }
}
