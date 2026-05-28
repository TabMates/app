package de.tabmates.features.tabgroup.presentation.navigation.addexpense

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.NewExpenseSplit
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.CurrencyPickerUiState
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.buildCurrencyPickerState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_error_amount_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_error_description_too_long
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_error_no_splits
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_error_paid_by_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_error_split_total_mismatch
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_error_title_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_error_title_too_long
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@KoinViewModel
class AddExpenseViewModel(
    @InjectedParam private val groupId: String,
    @InjectedParam private val expenseId: String,
    private val tabEntryRepository: TabEntryRepository,
    private val groupRepository: GroupRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val isEditing = expenseId.isNotBlank()
    private val currentUserId =
        sessionStorage
            .get()
            ?.user
            ?.id
            .orEmpty()
    private val _state =
        MutableStateFlow(
            AddExpenseState(groupId = groupId, currentUserId = currentUserId, isEditing = isEditing),
        )
    private var hasLoadedInitialData = false

    val state: StateFlow<AddExpenseState> =
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

    private val eventChannel = Channel<AddExpenseEvent>()
    val events = eventChannel.receiveAsFlow()

    private val currencyQueryFlow =
        snapshotFlow {
            _state.value.currencyQueryState.text
                .toString()
        }

    val currencyPickerState: StateFlow<CurrencyPickerUiState> =
        combine(state, currencyQueryFlow) { current, query ->
            buildCurrencyPickerState(
                currencies = current.supportedCurrencies,
                recentCodes = listOfNotNull(current.baseCurrencyCode.ifEmpty { null }),
                selectedCode = current.expenseCurrencyCode,
                query = query,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = CurrencyPickerUiState(),
        )

    fun onCurrencyClick() {
        _state.update { it.copy(isCurrencyPickerVisible = true) }
    }

    fun onCurrencyPickerDismiss() {
        _state.value.currencyQueryState.clearText()
        _state.update { it.copy(isCurrencyPickerVisible = false) }
    }

    fun onCurrencySelected(code: String) {
        val currency = _state.value.supportedCurrencies.firstOrNull { it.code == code }
        _state.value.currencyQueryState.clearText()
        _state.update {
            it.copy(
                expenseCurrencyCode = code,
                expenseCurrencySymbol = currency?.nativeSymbol ?: code,
                expenseCurrencyDecimalDigits = currency?.decimalDigits ?: 2,
                isCurrencyPickerVisible = false,
            )
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val group = groupRepository.getGroups().first().firstOrNull { it.id == groupId }
            val currencies = currencyRepository.getCurrencies().first()
            val rates = exchangeRateRepository.getExchangeRates().first()
            val activeMembers = group?.participants.orEmpty().toList()
            val existing =
                if (isEditing) {
                    tabEntryRepository.getTabEntryById(expenseId).first() as? TabEntry.Expense
                } else {
                    null
                }
            val baseCurrencyCode = group?.defaultCurrencyCode.orEmpty()
            val baseCurrency = currencies.firstOrNull { it.code == baseCurrencyCode }
            // Expense currency defaults to the group's base; an edited expense keeps its own.
            val expenseCurrencyCode = existing?.currencyCode ?: baseCurrencyCode
            val expenseCurrency = currencies.firstOrNull { it.code == expenseCurrencyCode }
            val decimals = expenseCurrency?.decimalDigits ?: 2
            val defaultPaidBy =
                existing?.paidByUserId
                    ?: activeMembers.firstOrNull { it.userId == currentUserId }?.userId
                    ?: activeMembers.firstOrNull()?.userId.orEmpty()
            val splitsByParticipant = existing?.splits?.associateBy { it.participantId }.orEmpty()
            _state.update {
                it.copy(
                    isLoading = false,
                    members = activeMembers,
                    paidByUserId = defaultPaidBy,
                    expenseCurrencyCode = expenseCurrencyCode,
                    expenseCurrencySymbol = expenseCurrency?.nativeSymbol ?: expenseCurrencyCode,
                    expenseCurrencyDecimalDigits = decimals,
                    baseCurrencyCode = baseCurrencyCode,
                    baseCurrencySymbol = baseCurrency?.nativeSymbol ?: baseCurrencyCode,
                    baseCurrencyDecimalDigits = baseCurrency?.decimalDigits ?: 2,
                    supportedCurrencies = currencies,
                    ratesByCurrency = rates.associate { it.currencyCode to it.rateToBase },
                    createdAt = existing?.createdAt ?: it.createdAt,
                    splitType = existing?.splits?.firstOrNull()?.splitType ?: it.splitType,
                    titleTextState = TextFieldState(existing?.title.orEmpty()),
                    descriptionTextState = TextFieldState(existing?.description.orEmpty()),
                    amountTextState =
                        TextFieldState(
                            existing?.let { e -> formatMoney("", e.amount, decimals) }.orEmpty(),
                        ),
                    splitInputs =
                        activeMembers.map { member ->
                            val split = splitsByParticipant[member.userId]
                            ParticipantSplitInput(
                                participantId = member.userId,
                                included = if (isEditing) split != null else true,
                                exactAmountState =
                                    TextFieldState(
                                        if (split?.splitType == SplitType.EXACT_AMOUNT) {
                                            formatMoney("", split.value, decimals)
                                        } else {
                                            ""
                                        },
                                    ),
                                percentageState =
                                    TextFieldState(
                                        if (split?.splitType == SplitType.PERCENTAGE) {
                                            formatMoney("", split.value, 2)
                                        } else {
                                            ""
                                        },
                                    ),
                                sharesState =
                                    TextFieldState(
                                        if (split?.splitType == SplitType.SHARES) {
                                            split.value.toLong().toString()
                                        } else {
                                            "1"
                                        },
                                    ),
                            )
                        },
                )
            }
        }
    }

    fun onPaidByClick() {
        _state.update { it.copy(isPaidByPickerVisible = true) }
    }

    fun onPaidByPickerDismiss() {
        _state.update { it.copy(isPaidByPickerVisible = false) }
    }

    fun onPaidBySelected(userId: String) {
        _state.update { it.copy(paidByUserId = userId, isPaidByPickerVisible = false) }
    }

    fun onSplitOpen() {
        _state.update { it.copy(isSplitEditorVisible = true) }
    }

    fun onSplitDismiss() {
        _state.update { it.copy(isSplitEditorVisible = false) }
    }

    fun onSplitTypeChange(type: SplitType) {
        _state.update { it.copy(splitType = type) }
    }

    fun onSplitParticipantToggle(participantId: String) {
        _state.update { current ->
            current.copy(
                splitInputs =
                    current.splitInputs.map { input ->
                        if (input.participantId == participantId) {
                            input.copy(included = !input.included)
                        } else {
                            input
                        }
                    },
            )
        }
    }

    fun onSplitConfirm() {
        _state.update { it.copy(isSplitEditorVisible = false) }
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
                createdAt = Instant.fromEpochMilliseconds(epochMillis),
                isDatePickerVisible = false,
            )
        }
    }

    fun onSaveClick() {
        if (_state.value.isSubmitting) return
        val current = _state.value
        val amount =
            parseAmount(current.amountTextState.text.toString())?.takeIf { it > 0.0 }
        if (amount == null) {
            emitError(UiText.Resource(Res.string.add_expense_error_amount_required))
            return
        }
        val title =
            current.titleTextState.text
                .toString()
                .trim()
        if (title.isEmpty()) {
            emitError(UiText.Resource(Res.string.add_expense_error_title_required))
            return
        }
        if (title.length > MAX_TITLE_LENGTH) {
            emitError(UiText.Resource(Res.string.add_expense_error_title_too_long))
            return
        }
        val description =
            current.descriptionTextState.text
                .toString()
                .trim()
        if (description.length > MAX_DESCRIPTION_LENGTH) {
            emitError(UiText.Resource(Res.string.add_expense_error_description_too_long))
            return
        }
        if (current.paidByUserId.isBlank()) {
            emitError(UiText.Resource(Res.string.add_expense_error_paid_by_required))
            return
        }
        val splits = buildSplits(current, amount) ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val result =
                if (isEditing) {
                    tabEntryRepository.updateExpense(
                        tabEntryId = expenseId,
                        groupId = current.groupId,
                        title = title,
                        description = description,
                        amount = amount,
                        currencyCode = current.expenseCurrencyCode,
                        paidByUserId = current.paidByUserId,
                        createdAt = current.createdAt,
                        splits = splits,
                    )
                } else {
                    tabEntryRepository.createExpense(
                        groupId = current.groupId,
                        title = title,
                        description = description,
                        amount = amount,
                        currencyCode = current.expenseCurrencyCode,
                        paidByUserId = current.paidByUserId,
                        createdAt = current.createdAt,
                        splits = splits,
                    )
                }
            result
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(AddExpenseEvent.ExpenseCreated)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(AddExpenseEvent.Error(error.toUiText()))
                }
        }
    }

    private fun buildSplits(
        state: AddExpenseState,
        totalAmount: Double,
    ): List<NewExpenseSplit>? {
        return when (state.splitType) {
            SplitType.EQUAL -> {
                val included = state.splitInputs.filter { it.included }
                if (included.isEmpty()) {
                    emitError(UiText.Resource(Res.string.add_expense_error_no_splits))
                    return null
                }
                included.map { NewExpenseSplit(it.participantId, SplitType.EQUAL, value = 1.0) }
            }

            SplitType.EXACT_AMOUNT -> {
                val rows =
                    state.splitInputs.map { input ->
                        input.participantId to (parseAmount(input.exactAmountState.text.toString()) ?: 0.0)
                    }
                val total = rows.sumOf { it.second }
                if (abs(total - totalAmount) > epsilon(state.expenseCurrencyDecimalDigits)) {
                    emitError(
                        UiText.Resource(
                            Res.string.add_expense_error_split_total_mismatch,
                            arrayOf(
                                formatMoney(
                                    state.expenseCurrencySymbol,
                                    totalAmount,
                                    state.expenseCurrencyDecimalDigits,
                                ),
                            ),
                        ),
                    )
                    return null
                }
                val nonZero = rows.filter { it.second > 0.0 }
                if (nonZero.isEmpty()) {
                    emitError(UiText.Resource(Res.string.add_expense_error_no_splits))
                    return null
                }
                nonZero.map { (id, value) -> NewExpenseSplit(id, SplitType.EXACT_AMOUNT, value) }
            }

            SplitType.PERCENTAGE -> {
                val rows =
                    state.splitInputs.map { input ->
                        input.participantId to (parseAmount(input.percentageState.text.toString()) ?: 0.0)
                    }
                val total = rows.sumOf { it.second }
                if (abs(total - 100.0) > 0.01) {
                    emitError(
                        UiText.Resource(
                            Res.string.add_expense_error_split_total_mismatch,
                            arrayOf("100%"),
                        ),
                    )
                    return null
                }
                val nonZero = rows.filter { it.second > 0.0 }
                if (nonZero.isEmpty()) {
                    emitError(UiText.Resource(Res.string.add_expense_error_no_splits))
                    return null
                }
                nonZero.map { (id, value) -> NewExpenseSplit(id, SplitType.PERCENTAGE, value) }
            }

            SplitType.SHARES -> {
                val rows =
                    state.splitInputs.map { input ->
                        input.participantId to (
                            input.sharesState.text
                                .toString()
                                .toIntOrNull()
                                ?.toDouble() ?: 0.0
                        )
                    }
                val total = rows.sumOf { it.second }
                if (total <= 0.0) {
                    emitError(UiText.Resource(Res.string.add_expense_error_no_splits))
                    return null
                }
                rows
                    .filter { it.second > 0.0 }
                    .map { (id, value) -> NewExpenseSplit(id, SplitType.SHARES, value) }
            }
        }
    }

    private fun emitError(message: UiText) {
        viewModelScope.launch { eventChannel.send(AddExpenseEvent.Error(message)) }
    }

    private fun epsilon(decimals: Int): Double {
        var v = 1.0
        repeat(decimals) { v /= 10.0 }
        return v / 2.0
    }

    private companion object {
        private const val MAX_TITLE_LENGTH = 255
        private const val MAX_DESCRIPTION_LENGTH = 255
    }
}
