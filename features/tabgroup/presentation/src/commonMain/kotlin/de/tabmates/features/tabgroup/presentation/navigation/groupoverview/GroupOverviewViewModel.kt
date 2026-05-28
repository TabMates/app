package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class GroupOverviewViewModel(
    groupRepository: GroupRepository,
    tabEntryRepository: TabEntryRepository,
    currencyRepository: CurrencyRepository,
    exchangeRateRepository: ExchangeRateRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val currentUser = sessionStorage.get()?.user
    private val currentUserId = currentUser?.id.orEmpty()

    private val searchQueryState = TextFieldState()
    private val searchQueryFlow = snapshotFlow { searchQueryState.text.toString() }

    private val filterFlow = MutableStateFlow(GroupFilter.ALL)
    private val selectedGroupIdFlow = MutableStateFlow<String?>(null)

    private val groupItemsFlow: Flow<List<GroupOverviewItem>> =
        combine(
            groupRepository.getGroups().onStart { emit(emptyList()) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
        ) { groups, currencies ->
            val currencyByCode = currencies.associateBy { it.code }
            groups
                .map { group -> group.toUiItem(currencyByCode[group.defaultCurrencyCode]) }
                .sortedByDescending { it.lastActivityAt }
        }.flatMapLatest { items ->
            if (items.isEmpty()) {
                flowOf(items)
            } else {
                enrichWithStats(items, tabEntryRepository, exchangeRateRepository.getExchangeRates())
            }
        }

    private fun enrichWithStats(
        baseItems: List<GroupOverviewItem>,
        tabEntryRepository: TabEntryRepository,
        ratesFlow: Flow<List<ExchangeRate>>,
    ): Flow<List<GroupOverviewItem>> {
        val entriesPerItem: Flow<List<Pair<GroupOverviewItem, List<TabEntry>>>> =
            combine(
                baseItems.map { item ->
                    tabEntryRepository
                        .getTabEntriesForGroup(item.id)
                        .onStart { emit(emptyList()) }
                        .map { entries -> item to entries }
                },
            ) { it.toList() }
        return combine(
            entriesPerItem,
            ratesFlow.onStart { emit(emptyList()) },
        ) { pairs, rates ->
            pairs
                .map { (item, entries) ->
                    item.withStats(entries, currentUserId, CurrencyConversion.from(item.currencyCode, rates))
                }.sortedByDescending { it.lastActivityAt }
        }.onStart { emit(baseItems) }
    }

    val state: StateFlow<GroupOverviewState> =
        combine(
            groupItemsFlow,
            filterFlow,
            searchQueryFlow,
            selectedGroupIdFlow,
        ) { items, filter, query, selected ->
            val displayed =
                items
                    .filter { filter.matches(it.balance) }
                    .filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
            GroupOverviewState(
                isLoading = false,
                allItems = items,
                displayedItems = displayed,
                filter = filter,
                searchQueryState = searchQueryState,
                selectedGroupId = selected ?: items.firstOrNull()?.id,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = GroupOverviewState(searchQueryState = searchQueryState),
        )

    fun onFilterSelected(filter: GroupFilter) {
        filterFlow.value = filter
    }

    fun onGroupSelected(groupId: String) {
        selectedGroupIdFlow.value = groupId
    }
}

private fun GroupFilter.matches(balance: GroupBalance): Boolean =
    when (this) {
        GroupFilter.ALL -> true
        GroupFilter.ACTIVE -> balance !is GroupBalance.Settled
        GroupFilter.SETTLED -> balance is GroupBalance.Settled
    }
