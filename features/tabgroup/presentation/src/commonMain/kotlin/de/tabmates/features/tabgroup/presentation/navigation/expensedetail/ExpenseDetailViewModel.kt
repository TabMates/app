package de.tabmates.features.tabgroup.presentation.navigation.expensedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.EntryKind
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ExpenseDetailViewModel(
    @InjectedParam private val expenseId: String,
    @InjectedParam private val groupId: String,
    private val tabEntryRepository: TabEntryRepository,
    groupRepository: GroupRepository,
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
    private val isDeleting = MutableStateFlow(false)

    val state: StateFlow<ExpenseDetailState> =
        combine(
            tabEntryRepository.getTabEntryById(expenseId).onStart { emit(null) },
            groupRepository.getGroups().onStart { emit(emptyList()) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
            exchangeRateRepository.getExchangeRates().onStart { emit(emptyList()) },
            isDeleting,
        ) { entry, groups, currencies, rates, deleting ->
            val group = groups.firstOrNull { it.id == groupId }
            // This screen renders split-carrying entries only (expense/income). A settlement id
            // lands here as null, matching the previous expense-only behaviour.
            val detailEntry =
                when (entry) {
                    is TabEntry.Expense, is TabEntry.Income -> entry
                    else -> null
                }
            val entryKind =
                when (detailEntry) {
                    is TabEntry.Income -> EntryKind.INCOME
                    else -> EntryKind.EXPENSE
                }
            val splits =
                when (detailEntry) {
                    is TabEntry.Expense -> detailEntry.splits
                    is TabEntry.Income -> detailEntry.splits
                    else -> emptyList()
                }
            val expenseCurrencyCode = detailEntry?.currencyCode ?: group?.defaultCurrencyCode.orEmpty()
            val expenseCurrency = currencies.firstOrNull { it.code == expenseCurrencyCode }
            val groupCurrencyCode = group?.defaultCurrencyCode.orEmpty()
            val groupCurrency = currencies.firstOrNull { it.code == groupCurrencyCode }
            ExpenseDetailState(
                expenseId = expenseId,
                isLoading = detailEntry == null,
                isDeleting = deleting,
                entry = detailEntry,
                entryKind = entryKind,
                splits = splits,
                currentUserId = currentUserId,
                expenseCurrencyCode = expenseCurrencyCode,
                expenseCurrencySymbol = expenseCurrency?.nativeSymbol ?: expenseCurrencyCode,
                expenseCurrencyDecimalDigits = expenseCurrency?.decimalDigits ?: 2,
                groupCurrencyCode = groupCurrencyCode,
                groupCurrencySymbol = groupCurrency?.nativeSymbol ?: groupCurrencyCode,
                groupCurrencyDecimalDigits = groupCurrency?.decimalDigits ?: 2,
                ratesByCurrency = rates.associate { it.currencyCode to it.rateToBase },
                ratesLastUpdatedAt = rates.maxOfOrNull { it.lastUpdatedAt },
                membersById =
                    group?.participants?.associateBy { it.userId }
                        ?: emptyMap(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ExpenseDetailState(expenseId = expenseId, currentUserId = currentUserId),
        )

    private val eventChannel = Channel<ExpenseDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onConfirmDelete() {
        if (isDeleting.value) return
        viewModelScope.launch {
            isDeleting.update { true }
            tabEntryRepository
                .deleteTabEntry(expenseId)
                .onSuccess { eventChannel.send(ExpenseDetailEvent.ExpenseDeleted) }
                .onFailure { error ->
                    eventChannel.send(ExpenseDetailEvent.Error(error.toUiText()))
                }
            isDeleting.update { false }
        }
    }
}
