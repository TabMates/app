package de.tabmates.features.tabgroup.presentation.navigation.entrydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.presentation.navigation.addentry.EntryKind
import de.tabmates.features.tabgroup.presentation.util.EntryLookup
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class EntryDetailViewModel(
    @InjectedParam private val entryId: String,
    @InjectedParam private val groupId: String,
    private val tabEntryRepository: TabEntryRepository,
    groupRepository: GroupRepository,
    currencyRepository: CurrencyRepository,
    exchangeRateRepository: ExchangeRateRepository,
    currentAccount: CurrentAccount,
) : ViewModel() {
    private val currentUserId =
        currentAccount.userId().orEmpty()
    private val isDeleting = MutableStateFlow(false)

    /**
     * Sticky, unlike [isDeleting]: deleting the entry from this screen empties the row the state
     * reads, so without this the screen would report the entry as missing on top of the
     * [EntryDetailEvent.EntryDeleted] it already sends.
     */
    private var deleteRequested = false

    /** The state recombines on every upstream change; the screen only needs telling once. */
    private var notifiedUnavailable = false

    private val eventChannel = Channel<EntryDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    val state: StateFlow<EntryDetailState> =
        combine(
            tabEntryRepository
                .getTabEntryById(entryId)
                .map<TabEntry?, EntryLookup> { EntryLookup.Loaded(it) }
                .onStart { emit(EntryLookup.Loading) },
            groupRepository.getGroups().onStart { emit(emptyList()) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
            exchangeRateRepository.getExchangeRates().onStart { emit(emptyList()) },
            isDeleting,
        ) { lookup, groups, currencies, rates, deleting ->
            val entry = (lookup as? EntryLookup.Loaded)?.entry
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
            val entryCurrencyCode = detailEntry?.currencyCode ?: group?.defaultCurrencyCode.orEmpty()
            val entryCurrency = currencies.firstOrNull { it.code == entryCurrencyCode }
            val groupCurrencyCode = group?.defaultCurrencyCode.orEmpty()
            val groupCurrency = currencies.firstOrNull { it.code == groupCurrencyCode }
            EntryDetailState(
                entryId = entryId,
                isLoading = lookup is EntryLookup.Loading,
                isMissing = lookup is EntryLookup.Loaded && detailEntry == null,
                isDeleting = deleting,
                entry = detailEntry,
                entryKind = entryKind,
                splits = splits,
                currentUserId = currentUserId,
                entryCurrencyCode = entryCurrencyCode,
                entryCurrencySymbol = entryCurrency?.nativeSymbol ?: entryCurrencyCode,
                entryCurrencyDecimalDigits = entryCurrency?.decimalDigits ?: 2,
                groupCurrencyCode = groupCurrencyCode,
                groupCurrencySymbol = groupCurrency?.nativeSymbol ?: groupCurrencyCode,
                groupCurrencyDecimalDigits = groupCurrency?.decimalDigits ?: 2,
                ratesByCurrency = rates.associate { it.currencyCode to it.rateToBase },
                ratesLastUpdatedAt = rates.maxOfOrNull { it.lastUpdatedAt },
                membersById =
                    group?.participants?.associateBy { it.userId }
                        ?: emptyMap(),
            )
        }.onEach { state ->
            // Sits before stateIn so the check only runs while the screen is actually collecting,
            // which also keeps it from opening a second subscription on the entry query.
            if (state.isMissing && !deleteRequested && !notifiedUnavailable) {
                notifiedUnavailable = true
                eventChannel.send(EntryDetailEvent.EntryUnavailable)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = EntryDetailState(entryId = entryId, currentUserId = currentUserId),
        )

    fun onConfirmDelete() {
        if (isDeleting.value) return
        deleteRequested = true
        viewModelScope.launch {
            isDeleting.update { true }
            tabEntryRepository
                .deleteTabEntry(entryId)
                .onSuccess { eventChannel.send(EntryDetailEvent.EntryDeleted) }
                .onFailure { error ->
                    eventChannel.send(EntryDetailEvent.Error(error.toUiText()))
                }
            isDeleting.update { false }
        }
    }
}
