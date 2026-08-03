package de.tabmates.features.tabgroup.presentation.navigation.editsettlement

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.core.presentation.format.formatAmountForInput
import de.tabmates.core.presentation.format.parseAmount
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_amount_required
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@KoinViewModel
class EditSettlementViewModel(
    @InjectedParam private val groupId: String,
    @InjectedParam private val settlementId: String,
    private val tabEntryRepository: TabEntryRepository,
    private val groupRepository: GroupRepository,
    private val currencyRepository: CurrencyRepository,
    currentAccount: CurrentAccount,
    private val numberSymbols: NumberSymbols,
) : ViewModel() {
    private val currentUserId =
        currentAccount.userId().orEmpty()
    private val _state =
        MutableStateFlow(EditSettlementState(settlementId = settlementId, currentUserId = currentUserId))
    private var hasLoadedInitialData = false

    val state: StateFlow<EditSettlementState> =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    hasLoadedInitialData = true
                    loadInitialData()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private val eventChannel = Channel<EditSettlementEvent>()
    val events = eventChannel.receiveAsFlow()

    private fun loadInitialData() {
        viewModelScope.launch {
            val group = groupRepository.getGroups().first().firstOrNull { it.id == groupId }
            val currencies = currencyRepository.getCurrencies().first()
            val settlement =
                tabEntryRepository.getTabEntryById(settlementId).first() as? TabEntry.Settlement
            val currencyCode = settlement?.currencyCode ?: group?.defaultCurrencyCode.orEmpty()
            val currency = currencies.firstOrNull { it.code == currencyCode }
            val decimals = currency?.decimalDigits ?: 2
            _state.update {
                it.copy(
                    isLoading = false,
                    amountTextState =
                        TextFieldState(
                            settlement
                                ?.let { s -> formatAmountForInput(s.amount, decimals, numberSymbols) }
                                .orEmpty(),
                        ),
                    entryDate = settlement?.entryDate ?: it.entryDate,
                    title = settlement?.title.orEmpty(),
                    description = settlement?.description.orEmpty(),
                    currencyCode = currencyCode,
                    currencySymbol = currency?.nativeSymbol ?: currencyCode,
                    currencyDecimalDigits = decimals,
                    exchangeRate = settlement?.exchangeRate,
                    paidByUserId = settlement?.paidByUserId.orEmpty(),
                    receivedByUserId = settlement?.receivedByUserId.orEmpty(),
                    membersById =
                        group?.participants?.associateBy { p -> p.userId }
                            ?: emptyMap(),
                )
            }
        }
    }

    fun onDateClick() {
        _state.update { it.copy(isDatePickerVisible = true) }
    }

    fun onDatePickerDismiss() {
        _state.update { it.copy(isDatePickerVisible = false) }
    }

    fun onDateSelected(epochMillis: Long) {
        _state.update {
            it.copy(
                entryDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date,
                isDatePickerVisible = false,
            )
        }
    }

    fun onSaveClick() {
        val current = _state.value
        if (current.isSubmitting || current.isLoading) return
        val amount =
            parseAmount(current.amountTextState.text.toString(), numberSymbols)?.takeIf { it > 0.0 }
        if (amount == null) {
            viewModelScope.launch {
                eventChannel.send(
                    EditSettlementEvent.Error(UiText.Resource(Res.string.add_entry_error_amount_required)),
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            tabEntryRepository
                .updateSettlement(
                    tabEntryId = settlementId,
                    groupId = groupId,
                    title = current.title,
                    description = current.description,
                    amount = amount,
                    currencyCode = current.currencyCode,
                    // Currency is not editable here, so the originally locked rate always stays.
                    exchangeRate = current.exchangeRate,
                    paidByUserId = current.paidByUserId,
                    receivedByUserId = current.receivedByUserId,
                    entryDate = current.entryDate,
                ).onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(EditSettlementEvent.SettlementSaved)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(EditSettlementEvent.Error(error.toUiText()))
                }
        }
    }
}
