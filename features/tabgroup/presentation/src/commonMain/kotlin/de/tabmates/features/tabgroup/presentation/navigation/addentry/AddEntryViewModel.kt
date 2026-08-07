package de.tabmates.features.tabgroup.presentation.navigation.addentry

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.core.presentation.format.amountEpsilon
import de.tabmates.core.presentation.format.formatAmountForInput
import de.tabmates.core.presentation.format.formatMoney
import de.tabmates.core.presentation.format.parseAmount
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyConverter
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.referencedParticipantIds
import de.tabmates.features.tabgroup.domain.tabentry.NewTabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_amount_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_description_too_long
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_no_splits
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_paid_by_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_split_total_mismatch
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_title_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_title_too_long
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@KoinViewModel
class AddEntryViewModel(
    @InjectedParam private val groupId: String,
    @InjectedParam private val entryId: String,
    private val tabEntryRepository: TabEntryRepository,
    private val groupRepository: GroupRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    currentAccount: CurrentAccount,
    private val numberSymbols: NumberSymbols,
) : ViewModel() {
    private val isEditing = entryId.isNotBlank()
    private val currentUserId =
        currentAccount.userId().orEmpty()
    private val _state =
        MutableStateFlow(
            AddEntryState(groupId = groupId, currentUserId = currentUserId, isEditing = isEditing),
        )
    private var hasLoadedInitialData = false

    val state: StateFlow<AddEntryState> =
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

    private val eventChannel = Channel<AddEntryEvent>()
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
                selectedCode = current.entryCurrencyCode,
                query = query,
                baseCurrencyCode = current.baseCurrencyCode,
                ratesByCurrency = current.ratesByCurrency,
                ratesLastUpdatedAt = current.ratesLastUpdatedAt,
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
                entryCurrencyCode = code,
                entryCurrencySymbol = currency?.nativeSymbol ?: code,
                entryCurrencyDecimalDigits = currency?.decimalDigits ?: 2,
                isCurrencyPickerVisible = false,
            )
        }
    }

    fun onKindChange(kind: EntryKind) {
        // Kind is locked once editing an existing entry — the server has separate update paths.
        if (isEditing) return
        _state.update { it.copy(entryKind = kind) }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val group = groupRepository.getGroups().first().firstOrNull { it.id == groupId }
            val currencies = currencyRepository.getCurrencies().first()
            val rates = exchangeRateRepository.getExchangeRates().first()
            val activeMembers = group?.participants.orEmpty().toList()
            // Edit mode loads an existing split-carrying entry (expense OR income); its kind is
            // then fixed for the rest of the edit. Create mode starts from the toggle default.
            val existing =
                if (isEditing) {
                    tabEntryRepository.getTabEntryById(entryId).first()
                } else {
                    null
                }
            val existingKind =
                when (existing) {
                    is TabEntry.Income -> EntryKind.INCOME
                    else -> EntryKind.EXPENSE
                }
            val existingSplits =
                when (existing) {
                    is TabEntry.Expense -> existing.splits
                    is TabEntry.Income -> existing.splits
                    else -> emptyList()
                }
            val baseCurrencyCode = group?.defaultCurrencyCode.orEmpty()
            val baseCurrency = currencies.firstOrNull { it.code == baseCurrencyCode }
            // Expense currency defaults to the group's base; an edited expense keeps its own.
            val entryCurrencyCode = existing?.currencyCode ?: baseCurrencyCode
            val entryCurrency = currencies.firstOrNull { it.code == entryCurrencyCode }
            val decimals = entryCurrency?.decimalDigits ?: 2
            val defaultPaidBy =
                existing?.paidByUserId
                    ?: activeMembers.firstOrNull { it.userId == currentUserId }?.userId
                    ?: activeMembers.firstOrNull()?.userId.orEmpty()
            val splitsByParticipant = existingSplits.associateBy { it.participantId }
            // An edited entry may reference people who have since been removed from the group.
            // They are not in [activeMembers], so building the split rows from membership alone
            // would drop their splits on save — the entry would silently lose money. Resolve them
            // from the global participant table instead and keep their rows editable.
            val activeMemberIds = activeMembers.map { it.userId }.toSet()
            val formerParticipantIds =
                listOfNotNull(existing).referencedParticipantIds() - activeMemberIds
            val formerParticipants =
                if (formerParticipantIds.isEmpty()) {
                    emptyList()
                } else {
                    groupRepository
                        .getAllParticipants()
                        .first()
                        .filter { it.userId in formerParticipantIds }
                }
            // Only former members who actually hold a split get a row; one who merely paid does
            // not become splittable, they just need to stay nameable.
            val splitParticipants =
                activeMembers + formerParticipants.filter { it.userId in splitsByParticipant }
            _state.update {
                it.copy(
                    isLoading = false,
                    entryKind = existingKind,
                    members = activeMembers,
                    participantsById =
                        (activeMembers + formerParticipants).associateBy { participant -> participant.userId },
                    formerParticipantIds = formerParticipantIds,
                    paidByUserId = defaultPaidBy,
                    entryCurrencyCode = entryCurrencyCode,
                    entryCurrencySymbol = entryCurrency?.nativeSymbol ?: entryCurrencyCode,
                    entryCurrencyDecimalDigits = decimals,
                    baseCurrencyCode = baseCurrencyCode,
                    baseCurrencySymbol = baseCurrency?.nativeSymbol ?: baseCurrencyCode,
                    baseCurrencyDecimalDigits = baseCurrency?.decimalDigits ?: 2,
                    supportedCurrencies = currencies,
                    ratesByCurrency = rates.associate { it.currencyCode to it.rateToBase },
                    ratesLastUpdatedAt = rates.maxOfOrNull { rate -> rate.lastUpdatedAt },
                    originalCurrencyCode = existing?.currencyCode.orEmpty(),
                    originalExchangeRate = existing?.exchangeRate,
                    entryDate = existing?.entryDate ?: it.entryDate,
                    splitType = existingSplits.firstOrNull()?.splitType ?: it.splitType,
                    titleTextState = TextFieldState(existing?.title.orEmpty()),
                    descriptionTextState = TextFieldState(existing?.description.orEmpty()),
                    amountTextState =
                        TextFieldState(
                            existing
                                ?.let { e -> formatAmountForInput(e.amount, decimals, numberSymbols) }
                                .orEmpty(),
                        ),
                    splitInputs =
                        splitParticipants.map { member ->
                            val split = splitsByParticipant[member.userId]
                            ParticipantSplitInput(
                                participantId = member.userId,
                                included = if (isEditing) split != null else true,
                                exactAmountState =
                                    TextFieldState(
                                        if (split?.splitType == SplitType.EXACT_AMOUNT) {
                                            formatAmountForInput(split.value, decimals, numberSymbols)
                                        } else {
                                            ""
                                        },
                                    ),
                                percentageState =
                                    TextFieldState(
                                        if (split?.splitType == SplitType.PERCENTAGE) {
                                            formatAmountForInput(split.value, PERCENTAGE_DECIMALS, numberSymbols)
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
                entryDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date,
                isDatePickerVisible = false,
            )
        }
    }

    fun onSaveClick() {
        if (_state.value.isSubmitting) return
        val current = _state.value
        val amount =
            parseAmount(current.amountTextState.text.toString(), numberSymbols)?.takeIf { it > 0.0 }
        if (amount == null) {
            emitError(UiText.Resource(Res.string.add_entry_error_amount_required))
            return
        }
        val title =
            current.titleTextState.text
                .toString()
                .trim()
        if (title.isEmpty()) {
            emitError(UiText.Resource(Res.string.add_entry_error_title_required))
            return
        }
        if (title.length > MAX_TITLE_LENGTH) {
            emitError(UiText.Resource(Res.string.add_entry_error_title_too_long))
            return
        }
        val description =
            current.descriptionTextState.text
                .toString()
                .trim()
        if (description.length > MAX_DESCRIPTION_LENGTH) {
            emitError(UiText.Resource(Res.string.add_entry_error_description_too_long))
            return
        }
        if (current.paidByUserId.isBlank()) {
            emitError(UiText.Resource(Res.string.add_entry_error_paid_by_required))
            return
        }
        val splits = buildSplits(current, amount) ?: return
        val exchangeRate = resolveExchangeRate(current)

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val isIncome = current.entryKind == EntryKind.INCOME
            val result =
                when {
                    isEditing && isIncome -> {
                        tabEntryRepository.updateIncome(
                            tabEntryId = entryId,
                            groupId = current.groupId,
                            title = title,
                            description = description,
                            amount = amount,
                            currencyCode = current.entryCurrencyCode,
                            exchangeRate = exchangeRate,
                            paidByUserId = current.paidByUserId,
                            entryDate = current.entryDate,
                            splits = splits,
                        )
                    }

                    isEditing -> {
                        tabEntryRepository.updateExpense(
                            tabEntryId = entryId,
                            groupId = current.groupId,
                            title = title,
                            description = description,
                            amount = amount,
                            currencyCode = current.entryCurrencyCode,
                            exchangeRate = exchangeRate,
                            paidByUserId = current.paidByUserId,
                            entryDate = current.entryDate,
                            splits = splits,
                        )
                    }

                    isIncome -> {
                        tabEntryRepository.createIncome(
                            groupId = current.groupId,
                            title = title,
                            description = description,
                            amount = amount,
                            currencyCode = current.entryCurrencyCode,
                            exchangeRate = exchangeRate,
                            paidByUserId = current.paidByUserId,
                            entryDate = current.entryDate,
                            splits = splits,
                        )
                    }

                    else -> {
                        tabEntryRepository.createExpense(
                            groupId = current.groupId,
                            title = title,
                            description = description,
                            amount = amount,
                            currencyCode = current.entryCurrencyCode,
                            exchangeRate = exchangeRate,
                            paidByUserId = current.paidByUserId,
                            entryDate = current.entryDate,
                            splits = splits,
                        )
                    }
                }
            result
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(AddEntryEvent.EntrySaved)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(AddEntryEvent.Error(error.toUiText()))
                }
        }
    }

    /**
     * The rate locked onto the expense at save time (group base currency per 1 unit of the
     * expense currency) — the same value the rate hint on screen shows, so what the user sees is
     * what gets locked. Editing keeps the originally locked rate unless the currency changed;
     * same-currency expenses and missing rates yield null (consumers fall back to live rates).
     */
    private fun resolveExchangeRate(state: AddEntryState): Double? {
        if (isEditing && state.entryCurrencyCode == state.originalCurrencyCode) {
            return state.originalExchangeRate
        }
        if (state.entryCurrencyCode == state.baseCurrencyCode) return null
        return CurrencyConverter.convert(
            amount = 1.0,
            from = state.entryCurrencyCode,
            to = state.baseCurrencyCode,
            rates = state.ratesByCurrency,
        )
    }

    private fun buildSplits(
        state: AddEntryState,
        totalAmount: Double,
    ): List<NewTabEntrySplit>? {
        return when (state.splitType) {
            SplitType.EQUAL -> {
                val included = state.splitInputs.filter { it.included }
                if (included.isEmpty()) {
                    emitError(UiText.Resource(Res.string.add_entry_error_no_splits))
                    return null
                }
                included.map { NewTabEntrySplit(it.participantId, SplitType.EQUAL, value = 1.0) }
            }

            SplitType.EXACT_AMOUNT -> {
                val rows =
                    state.splitInputs.map { input ->
                        input.participantId to
                            (parseAmount(input.exactAmountState.text.toString(), numberSymbols) ?: 0.0)
                    }
                val total = rows.sumOf { it.second }
                if (abs(total - totalAmount) > amountEpsilon(state.entryCurrencyDecimalDigits)) {
                    emitError(
                        UiText.Resource(
                            Res.string.add_entry_error_split_total_mismatch,
                            arrayOf(
                                formatMoney(
                                    state.entryCurrencySymbol,
                                    totalAmount,
                                    state.entryCurrencyDecimalDigits,
                                    numberSymbols,
                                ),
                            ),
                        ),
                    )
                    return null
                }
                val nonZero = rows.filter { it.second > 0.0 }
                if (nonZero.isEmpty()) {
                    emitError(UiText.Resource(Res.string.add_entry_error_no_splits))
                    return null
                }
                nonZero.map { (id, value) -> NewTabEntrySplit(id, SplitType.EXACT_AMOUNT, value) }
            }

            SplitType.PERCENTAGE -> {
                val rows =
                    state.splitInputs.map { input ->
                        input.participantId to
                            (parseAmount(input.percentageState.text.toString(), numberSymbols) ?: 0.0)
                    }
                val total = rows.sumOf { it.second }
                if (abs(total - 100.0) > 0.01) {
                    emitError(
                        UiText.Resource(
                            Res.string.add_entry_error_split_total_mismatch,
                            arrayOf("100%"),
                        ),
                    )
                    return null
                }
                val nonZero = rows.filter { it.second > 0.0 }
                if (nonZero.isEmpty()) {
                    emitError(UiText.Resource(Res.string.add_entry_error_no_splits))
                    return null
                }
                nonZero.map { (id, value) -> NewTabEntrySplit(id, SplitType.PERCENTAGE, value) }
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
                    emitError(UiText.Resource(Res.string.add_entry_error_no_splits))
                    return null
                }
                rows
                    .filter { it.second > 0.0 }
                    .map { (id, value) -> NewTabEntrySplit(id, SplitType.SHARES, value) }
            }
        }
    }

    private fun emitError(message: UiText) {
        viewModelScope.launch { eventChannel.send(AddEntryEvent.Error(message)) }
    }

    private companion object {
        private const val MAX_TITLE_LENGTH = 255
        private const val MAX_DESCRIPTION_LENGTH = 255
    }
}
