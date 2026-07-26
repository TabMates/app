package de.tabmates.features.tabgroup.presentation.navigation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.currency.CurrencyConverter
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.byMostRecentActivity
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.toUiItem
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.withStats
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
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class HomeViewModel(
    groupRepository: GroupRepository,
    tabEntryRepository: TabEntryRepository,
    currencyRepository: CurrencyRepository,
    exchangeRateRepository: ExchangeRateRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val currentUserId =
        sessionStorage
            .get()
            ?.user
            ?.id
            .orEmpty()

    val state: StateFlow<HomeState> =
        combine(
            groupRepository.getGroups(),
            currencyRepository.getCurrencies(),
            exchangeRateRepository.getExchangeRates(),
        ) { groups, currencies, rates -> Triple(groups, currencies, rates) }
            .flatMapLatest { (groups, currencies, rates) ->
                if (groups.isEmpty()) {
                    flowOf(HomeState(isLoading = false))
                } else {
                    combine(
                        groups.map { group ->
                            tabEntryRepository
                                .getTabEntriesForGroup(group.id)
                                .map { entries -> group to entries }
                        },
                    ) { groupEntries -> buildState(groupEntries.toList(), currencies, rates) }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = HomeState(),
            )

    private fun buildState(
        groupEntries: List<Pair<Group, List<TabEntry>>>,
        currencies: List<Currency>,
        rates: List<ExchangeRate>,
    ): HomeState {
        val currencyByCode = currencies.associateBy { it.code }
        val ratesByCurrency = rates.associate { it.currencyCode to it.rateToBase }

        val itemsByGroup =
            groupEntries.map { (group, entries) ->
                group to
                    group
                        .toUiItem(currencyByCode[group.defaultCurrencyCode])
                        .withStats(
                            entries,
                            currentUserId,
                            CurrencyConversion.from(group.defaultCurrencyCode, rates),
                        )
            }
        val contributing = itemsByGroup.filter { (_, item) -> item.balance !is GroupBalance.Settled }

        // Show the net in the currency the user's actual (non-settled) balances are in, so empty or
        // settled groups in another currency don't change the shown symbol. Fall back to the most
        // common group currency only when nothing is owed either way.
        val displayCode =
            contributing
                .ifEmpty { itemsByGroup }
                .map { it.first.defaultCurrencyCode }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                .orEmpty()
        val displayCurrency = currencyByCode[displayCode]

        val netTotal =
            itemsByGroup.sumOf { (group, item) ->
                CurrencyConverter.convert(
                    amount = item.balance.signedAmount(),
                    from = group.defaultCurrencyCode,
                    to = displayCode,
                    rates = ratesByCurrency,
                ) ?: 0.0
            }

        return HomeState(
            isLoading = false,
            netAmount = netTotal,
            displaySymbol = displayCurrency?.nativeSymbol ?: displayCode,
            displayDecimals = displayCurrency?.decimalDigits ?: DEFAULT_DECIMALS,
            contributingGroupCount = contributing.size,
            topGroups =
                itemsByGroup
                    .sortedWith(compareBy(byMostRecentActivity) { (_, item) -> item })
                    .take(MAX_TOP_GROUPS)
                    .map { (group, item) ->
                        HomeGroup(
                            id = group.id,
                            title = group.title,
                            iconKey = item.iconKey,
                            colorKey = item.colorKey,
                            memberCount = group.participants.size,
                            balance = item.balance,
                            currencySymbol = item.currencySymbol,
                            currencyDecimals = item.currencyDecimalDigits,
                            memberInitials = group.participants.take(MAX_AVATARS).map { it.initials },
                            hasPendingSync = item.hasPendingSync,
                        )
                    },
        )
    }

    private fun GroupBalance.signedAmount(): Double =
        when (this) {
            is GroupBalance.Owed -> amount
            is GroupBalance.Owe -> -amount
            GroupBalance.Settled -> 0.0
        }

    private companion object {
        private const val MAX_TOP_GROUPS = 3
        private const val MAX_AVATARS = 3
        private const val DEFAULT_DECIMALS = 2
    }
}
