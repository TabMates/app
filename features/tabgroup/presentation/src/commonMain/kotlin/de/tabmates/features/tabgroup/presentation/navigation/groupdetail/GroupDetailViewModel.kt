package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import de.tabmates.features.tabgroup.domain.balance.PerPersonBalanceCalculator
import de.tabmates.features.tabgroup.domain.balance.UserBalanceCalculator
import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.presentation.navigation.activity.ActivityFeedBuilder
import de.tabmates.features.tabgroup.presentation.navigation.activity.ActivitySection
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupOverviewItem
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.toUiItem
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.withStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

data class GroupDetailState(
    val item: GroupOverviewItem? = null,
    val currentUserId: String = "",
    val members: List<GroupParticipant> = emptyList(),
    /** Entries shown in the transaction list: expenses, incomes and settlements, newest first. */
    val entries: List<TabEntry> = emptyList(),
    val perPersonBalances: Map<String, Double> = emptyMap(),
    /** Each member's overall net in the group (positive = gets money back, negative = owes). */
    val memberNetBalances: Map<String, Double> = emptyMap(),
    /** True while any member's overall net is not settled — gates the Settle Up entry point. */
    val hasOutstandingDebts: Boolean = false,
    val currencyByCode: Map<String, Currency> = emptyMap(),
    val ratesByCurrency: Map<String, Double> = emptyMap(),
    /** This group's activity log, newest first, for the History tab. */
    val historySections: List<ActivitySection> = emptyList(),
    val canLoadMoreHistory: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class GroupDetailViewModel(
    @InjectedParam private val groupId: String,
    private val groupRepository: GroupRepository,
    private val tabEntryRepository: TabEntryRepository,
    currencyRepository: CurrencyRepository,
    exchangeRateRepository: ExchangeRateRepository,
    activityRepository: ActivityRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val currentUser = sessionStorage.get()?.user
    private val currentUserId = currentUser?.id.orEmpty()
    private val isRotatingInvite = MutableStateFlow(false)
    private val historyPageSize = MutableStateFlow(INITIAL_HISTORY_PAGE_SIZE)

    /**
     * Pre-combined so the state builder stays within the typed `combine` overloads. Names come from
     * every known participant, not the group's members, so a diff can still name someone who left.
     */
    private val history: Flow<HistoryInput> =
        combine(
            historyPageSize.flatMapLatest { limit -> activityRepository.observeGroupFeed(groupId, limit) },
            groupRepository.getAllParticipants(),
            historyPageSize,
        ) { feed, participants, limit -> HistoryInput(feed, participants, limit) }
            .onStart { emit(HistoryInput()) }

    val state: StateFlow<GroupDetailState> =
        combine(
            groupRepository
                .getGroups()
                .map { groups -> groups.firstOrNull { it.id == groupId } }
                .onStart { emit(null) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
            tabEntryRepository
                .getTabEntriesForGroup(groupId)
                .onStart { emit(emptyList()) },
            exchangeRateRepository.getExchangeRates().onStart { emit(emptyList()) },
            history,
        ) { group, currencies, entries, rates, history ->
            val visibleEntries = entries.filterNot { it.isDeleted }
            val conversion = group?.let { CurrencyConversion.from(it.defaultCurrencyCode, rates) }
            val memberNetBalances =
                group?.participants.orEmpty().associate { participant ->
                    participant.userId to
                        UserBalanceCalculator.computeNet(visibleEntries, participant.userId, conversion)
                }
            val item =
                group?.let {
                    val currency = currencies.firstOrNull { c -> c.code == it.defaultCurrencyCode }
                    it.toUiItem(currency).withStats(entries, currentUserId, conversion)
                }
            GroupDetailState(
                item = item,
                currentUserId = currentUserId,
                members = group?.participants?.toList().orEmpty(),
                entries =
                    visibleEntries
                        .filter {
                            it is TabEntry.Expense || it is TabEntry.Settlement || it is TabEntry.Income
                        }.sortedWith(
                            compareByDescending<TabEntry> { it.entryDate }
                                .thenByDescending { it.createdAt },
                        ),
                perPersonBalances =
                    PerPersonBalanceCalculator.computeByParticipant(visibleEntries, currentUserId, conversion),
                memberNetBalances = memberNetBalances,
                hasOutstandingDebts =
                    memberNetBalances.values.any { GroupBalance.fromNet(it) != GroupBalance.Settled },
                currencyByCode = currencies.associateBy { it.code },
                ratesByCurrency = rates.associate { it.currencyCode to it.rateToBase },
                historySections =
                    ActivityFeedBuilder.build(
                        items = history.feed,
                        currentUserId = currentUserId,
                        groupTitles = group?.let { mapOf(it.id to it.title) }.orEmpty(),
                        participantNames = history.participants.associate { it.userId to it.username },
                        currencyByCode = currencies.associateBy { it.code },
                        now = Clock.System.now(),
                        // The group name is the screen's own title; repeating it in every row is noise.
                        includeGroupName = false,
                    ),
                canLoadMoreHistory =
                    history.feed.count { it is ActivityFeedItem.Persisted } >= history.limit,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = GroupDetailState(),
        )

    fun rotateInvite() {
        if (groupId.isBlank() || isRotatingInvite.value) return
        viewModelScope.launch {
            isRotatingInvite.update { true }
            groupRepository.rotateInviteToken(groupId)
            isRotatingInvite.update { false }
        }
    }

    fun loadMoreHistory() {
        historyPageSize.update { it + HISTORY_PAGE_SIZE_INCREMENT }
    }

    private data class HistoryInput(
        val feed: List<ActivityFeedItem> = emptyList(),
        val participants: List<GroupParticipant> = emptyList(),
        val limit: Int = INITIAL_HISTORY_PAGE_SIZE,
    )

    private companion object {
        private const val INITIAL_HISTORY_PAGE_SIZE = 40
        private const val HISTORY_PAGE_SIZE_INCREMENT = 40
    }
}
