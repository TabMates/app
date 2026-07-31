package de.tabmates.features.tabgroup.presentation.navigation.settlementdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
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
class SettlementDetailViewModel(
    @InjectedParam private val settlementId: String,
    @InjectedParam private val groupId: String,
    private val tabEntryRepository: TabEntryRepository,
    groupRepository: GroupRepository,
    currencyRepository: CurrencyRepository,
    currentAccount: CurrentAccount,
) : ViewModel() {
    private val currentUserId =
        currentAccount.userId().orEmpty()
    private val isDeleting = MutableStateFlow(false)

    /**
     * Sticky, unlike [isDeleting]: deleting the settlement from this screen empties the row the
     * state reads, so without this the screen would report it as missing on top of the
     * [SettlementDetailEvent.SettlementDeleted] it already sends.
     */
    private var deleteRequested = false

    /** The state recombines on every upstream change; the screen only needs telling once. */
    private var notifiedUnavailable = false

    private val eventChannel = Channel<SettlementDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    val state: StateFlow<SettlementDetailState> =
        combine(
            tabEntryRepository
                .getTabEntryById(settlementId)
                .map<TabEntry?, EntryLookup> { EntryLookup.Loaded(it) }
                .onStart { emit(EntryLookup.Loading) },
            groupRepository.getGroups().onStart { emit(emptyList()) },
            currencyRepository.getCurrencies().onStart { emit(emptyList()) },
            isDeleting,
        ) { lookup, groups, currencies, deleting ->
            val group = groups.firstOrNull { it.id == groupId }
            val settlement = (lookup as? EntryLookup.Loaded)?.entry as? TabEntry.Settlement
            val currencyCode = settlement?.currencyCode ?: group?.defaultCurrencyCode.orEmpty()
            val currency = currencies.firstOrNull { it.code == currencyCode }
            SettlementDetailState(
                settlementId = settlementId,
                isLoading = lookup is EntryLookup.Loading,
                isMissing = lookup is EntryLookup.Loaded && settlement == null,
                isDeleting = deleting,
                settlement = settlement,
                currentUserId = currentUserId,
                groupCurrencySymbol = currency?.nativeSymbol ?: currencyCode,
                groupCurrencyDecimalDigits = currency?.decimalDigits ?: 2,
                membersById =
                    group?.participants?.associateBy { it.userId }
                        ?: emptyMap(),
            )
        }.onEach { state ->
            // Sits before stateIn so the check only runs while the screen is actually collecting,
            // which also keeps it from opening a second subscription on the entry query.
            if (state.isMissing && !deleteRequested && !notifiedUnavailable) {
                notifiedUnavailable = true
                eventChannel.send(SettlementDetailEvent.SettlementUnavailable)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SettlementDetailState(settlementId = settlementId, currentUserId = currentUserId),
        )

    fun onConfirmDelete() {
        if (isDeleting.value) return
        deleteRequested = true
        viewModelScope.launch {
            isDeleting.update { true }
            tabEntryRepository
                .deleteTabEntry(settlementId)
                .onSuccess { eventChannel.send(SettlementDetailEvent.SettlementDeleted) }
                .onFailure { error ->
                    eventChannel.send(SettlementDetailEvent.Error(error.toUiText()))
                }
            isDeleting.update { false }
        }
    }
}
