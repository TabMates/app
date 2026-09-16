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
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.domain.models.referencedParticipantIds
import de.tabmates.features.tabgroup.domain.recurring.NewRecurringTemplateSplit
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.domain.recurring.RecurringEntryType
import de.tabmates.features.tabgroup.domain.recurring.RecurringOccurrenceCalculator
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplate
import de.tabmates.features.tabgroup.domain.sync.ConnectionStatusRepository
import de.tabmates.features.tabgroup.domain.tabentry.NewTabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.SplitResolver
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_amount_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_description_too_long
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_effective_from_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_no_splits
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_paid_by_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_received_by_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_same_payer_and_receiver
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_split_total_mismatch
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_title_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_error_title_too_long
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@KoinViewModel
class AddEntryViewModel(
    @InjectedParam private val groupId: String,
    @InjectedParam private val entryId: String,
    @InjectedParam private val seriesId: String,
    private val tabEntryRepository: TabEntryRepository,
    private val recurringSeriesRepository: RecurringSeriesRepository,
    connectionStatusRepository: ConnectionStatusRepository,
    private val groupRepository: GroupRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    currentAccount: CurrentAccount,
    private val numberSymbols: NumberSymbols,
) : ViewModel() {
    private val isEditingSeries = seriesId.isNotBlank()

    /** True only for an existing *entry* — the one case that has a row to load and update. */
    private val isEditingEntry = entryId.isNotBlank()

    /**
     * True while the form is bound to something that already exists, entry or schedule. Locks
     * the kind toggle in both cases: the server has separate update paths per entry type, and a
     * series' type is fixed for its whole life.
     */
    private val isEditing = isEditingEntry || isEditingSeries
    private val currentUserId =
        currentAccount.userId().orEmpty()
    private val _state =
        MutableStateFlow(
            AddEntryState(
                groupId = groupId,
                currentUserId = currentUserId,
                isEditing = isEditing,
                editingSeriesId = seriesId.takeIf { isEditingSeries },
            ),
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

    init {
        // Schedules are written straight to the server with no outbox behind them, so the repeat
        // controls follow the live connection instead of queueing a standing instruction on a
        // device that may not be online again for days.
        connectionStatusRepository.isConnected
            .onEach { connected -> _state.update { it.copy(isOnline = connected) } }
            .launchIn(viewModelScope)
    }

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
            // Gated on the entry id alone — a schedule edit carries no entry to look up.
            val existing =
                if (isEditingEntry) {
                    tabEntryRepository.getTabEntryById(entryId).first()
                } else {
                    null
                }
            val series =
                if (isEditingSeries) {
                    recurringSeriesRepository.getSeriesById(seriesId).first()
                } else {
                    null
                }
            val existingKind =
                when {
                    series != null -> {
                        when (series.entryType) {
                            RecurringEntryType.EXPENSE -> EntryKind.EXPENSE
                            RecurringEntryType.INCOME -> EntryKind.INCOME
                            RecurringEntryType.SETTLEMENT -> EntryKind.SETTLEMENT
                        }
                    }

                    existing is TabEntry.Income -> {
                        EntryKind.INCOME
                    }

                    existing is TabEntry.Settlement -> {
                        EntryKind.SETTLEMENT
                    }

                    else -> {
                        EntryKind.EXPENSE
                    }
                }
            // The server only accepts an edit anchored on a future date the schedule actually
            // produces, so the picker is built from the schedule itself rather than a calendar.
            val effectiveFromOptions =
                series
                    ?.let {
                        RecurringOccurrenceCalculator.upcomingOccurrences(
                            rule = it.rule,
                            after = todayUtc(),
                            limit = EFFECTIVE_FROM_OPTION_COUNT,
                            skippedDates = it.skippedOccurrenceDates,
                        )
                    }.orEmpty()
            // A schedule's splits live on its template, not on any entry. Reading only [existing]
            // here left a series edit with no splits at all, which unchecks every row below and
            // saves the schedule with nobody on it.
            val existingSplits =
                series?.rule?.splits?.map { SplitSeed(it.participantId, it.splitType, it.value) }
                    ?: when (existing) {
                        is TabEntry.Expense -> existing.splits.map { it.toSeed() }
                        is TabEntry.Income -> existing.splits.map { it.toSeed() }
                        else -> emptyList()
                    }
            val baseCurrencyCode = group?.defaultCurrencyCode.orEmpty()
            val baseCurrency = currencies.firstOrNull { it.code == baseCurrencyCode }
            // Expense currency defaults to the group's base; an edited expense keeps its own.
            val entryCurrencyCode = series?.rule?.currencyCode ?: existing?.currencyCode ?: baseCurrencyCode
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
            // A parked schedule is one naming somebody who left, and this form is where it gets
            // repaired — so the template's own people have to be resolvable too, not just an
            // entry's.
            val activeMemberIds = activeMembers.map { it.userId }.toSet()
            val referencedIds =
                listOfNotNull(existing).referencedParticipantIds() +
                    existingSplits.map { it.participantId } +
                    listOfNotNull(series?.rule?.paidByUserId, series?.rule?.receivedByUserId)
            val formerParticipantIds = referencedIds - activeMemberIds
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
                    paidByUserId = series?.rule?.paidByUserId ?: defaultPaidBy,
                    receivedByUserId =
                        series?.rule?.receivedByUserId
                            ?: (existing as? TabEntry.Settlement)?.receivedByUserId
                            ?: activeMembers.firstOrNull { m -> m.userId != defaultPaidBy }?.userId.orEmpty(),
                    repeatFrequency = series?.rule?.frequency,
                    repeatInterval = series?.rule?.interval ?: 1,
                    repeatStartDate = series?.rule?.startDate ?: it.repeatStartDate,
                    repeatEnd = series?.rule?.end ?: it.repeatEnd,
                    effectiveFromOptions = effectiveFromOptions,
                    effectiveFrom = effectiveFromOptions.firstOrNull(),
                    entryCurrencyCode = entryCurrencyCode,
                    entryCurrencySymbol = entryCurrency?.nativeSymbol ?: entryCurrencyCode,
                    entryCurrencyDecimalDigits = decimals,
                    baseCurrencyCode = baseCurrencyCode,
                    baseCurrencySymbol = baseCurrency?.nativeSymbol ?: baseCurrencyCode,
                    baseCurrencyDecimalDigits = baseCurrency?.decimalDigits ?: 2,
                    supportedCurrencies = currencies,
                    ratesByCurrency = rates.associate { it.currencyCode to it.rateToBase },
                    ratesLastUpdatedAt = rates.maxOfOrNull { rate -> rate.lastUpdatedAt },
                    originalCurrencyCode = series?.rule?.currencyCode ?: existing?.currencyCode.orEmpty(),
                    originalExchangeRate = series?.rule?.exchangeRate ?: existing?.exchangeRate,
                    entryDate = effectiveFromOptions.firstOrNull() ?: existing?.entryDate ?: it.entryDate,
                    splitType = existingSplits.firstOrNull()?.splitType ?: it.splitType,
                    titleTextState = TextFieldState(series?.rule?.title ?: existing?.title.orEmpty()),
                    descriptionTextState =
                        TextFieldState(series?.rule?.description ?: existing?.description.orEmpty()),
                    amountTextState =
                        TextFieldState(
                            (series?.rule?.amount ?: existing?.amount)
                                ?.let { amount -> formatAmountForInput(amount, decimals, numberSymbols) }
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

    fun onReceivedByClick() {
        _state.update { it.copy(isReceivedByPickerVisible = true) }
    }

    fun onReceivedByPickerDismiss() {
        _state.update { it.copy(isReceivedByPickerVisible = false) }
    }

    fun onReceivedBySelected(userId: String) {
        _state.update { it.copy(receivedByUserId = userId, isReceivedByPickerVisible = false) }
    }

    /**
     * Opens the repeat editor, seeding its start date from the entry's own date.
     *
     * Only when that date is still in the future: a schedule may not start in the past, so
     * inheriting a back-dated entry's date would open the editor already invalid.
     */
    fun onRepeatOpen() {
        _state.update { current ->
            val today = todayUtc()
            current.copy(
                isRepeatEditorVisible = true,
                repeatStartDate =
                    if (current.repeatFrequency == null) {
                        maxOf(current.entryDate, today)
                    } else {
                        current.repeatStartDate
                    },
            )
        }
    }

    /**
     * Closes the repeat editor and lines the entry date up with the schedule.
     *
     * For a schedule the two mean the same thing — the first occurrence — so leaving them apart
     * would show a date the series is never going to produce.
     */
    fun onRepeatDismiss() {
        _state.update { current ->
            current.copy(
                isRepeatEditorVisible = false,
                entryDate = if (current.repeatFrequency != null) current.repeatStartDate else current.entryDate,
            )
        }
    }

    /** Null clears the repeat, which is what makes the form save a one-off entry again. */
    fun onRepeatFrequencyChange(frequency: RecurrenceFrequency?) {
        _state.update { it.copy(repeatFrequency = frequency) }
    }

    fun onRepeatIntervalChange(interval: Int) {
        _state.update { it.copy(repeatInterval = interval.coerceAtLeast(1)) }
    }

    /** Clamped to today: the server refuses a schedule that reaches back into the past. */
    fun onRepeatStartDateChange(date: LocalDate) {
        _state.update {
            it.copy(repeatStartDate = maxOf(date, todayUtc()), isRepeatStartPickerVisible = false)
        }
    }

    /**
     * The date picker offers the whole calendar, including days before the schedule starts. An end
     * that early describes a series that produces nothing at all, so it is pulled forward to the
     * start date — one occurrence — rather than saved as written.
     */
    fun onRepeatEndChange(end: RecurringEnd) {
        _state.update { current ->
            val clamped =
                when (end) {
                    is RecurringEnd.Until -> RecurringEnd.Until(maxOf(end.date, current.repeatStartDate))
                    else -> end
                }
            current.copy(repeatEnd = clamped, isRepeatEndPickerVisible = false)
        }
    }

    fun onRepeatStartPickerOpen() {
        _state.update { it.copy(isRepeatStartPickerVisible = true) }
    }

    fun onRepeatStartPickerDismiss() {
        _state.update { it.copy(isRepeatStartPickerVisible = false) }
    }

    fun onRepeatEndPickerOpen() {
        _state.update { it.copy(isRepeatEndPickerVisible = true) }
    }

    fun onRepeatEndPickerDismiss() {
        _state.update { it.copy(isRepeatEndPickerVisible = false) }
    }

    fun onEffectiveFromClick() {
        _state.update { it.copy(isEffectiveFromPickerVisible = true) }
    }

    fun onEffectiveFromPickerDismiss() {
        _state.update { it.copy(isEffectiveFromPickerVisible = false) }
    }

    /**
     * Picks the occurrence a schedule edit takes effect from.
     *
     * The new template's start date has to equal it — the server rejects any other pairing, because
     * an edit anchored anywhere else silently re-times the whole series to whichever day the edit
     * was made.
     */
    fun onEffectiveFromSelected(date: LocalDate) {
        _state.update { current ->
            current.copy(
                effectiveFrom = date,
                entryDate = date,
                repeatStartDate = date,
                isEffectiveFromPickerVisible = false,
            )
        }
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
        if (current.isSettlement) {
            if (current.receivedByUserId.isBlank()) {
                emitError(UiText.Resource(Res.string.add_entry_error_received_by_required))
                return
            }
            // The server refuses this too, but a settlement from someone to themselves is a typo
            // worth catching on the form rather than as a round trip.
            if (current.receivedByUserId == current.paidByUserId) {
                emitError(UiText.Resource(Res.string.add_entry_error_same_payer_and_receiver))
                return
            }
        }
        // Settlements carry no splits; the other two must reconcile to the total.
        val splits = if (current.isSettlement) emptyList() else buildSplits(current, amount) ?: return
        val exchangeRate = resolveExchangeRate(current)

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            if (current.repeat != null) {
                saveSeries(current, title, description, amount, splits, exchangeRate)
                return@launch
            }
            val isIncome = current.entryKind == EntryKind.INCOME
            val isSettlement = current.isSettlement
            val result =
                when {
                    isEditing && isSettlement -> {
                        tabEntryRepository.updateSettlement(
                            tabEntryId = entryId,
                            groupId = current.groupId,
                            title = title,
                            description = description,
                            amount = amount,
                            currencyCode = current.entryCurrencyCode,
                            exchangeRate = exchangeRate,
                            paidByUserId = current.paidByUserId,
                            receivedByUserId = current.receivedByUserId,
                            entryDate = current.entryDate,
                        )
                    }

                    isSettlement -> {
                        tabEntryRepository.createSettlement(
                            groupId = current.groupId,
                            title = title,
                            description = description,
                            amount = amount,
                            currencyCode = current.entryCurrencyCode,
                            exchangeRate = exchangeRate,
                            paidByUserId = current.paidByUserId,
                            receivedByUserId = current.receivedByUserId,
                            entryDate = current.entryDate,
                        )
                    }

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
     * Writes a recurring schedule instead of an entry.
     *
     * Creating a schedule does **not** also write today's entry: the server owns occurrence
     * generation, and it will write the first one itself on its next sweep. Doing both here would
     * book the same rent twice, once by hand and once by the sweep, in everybody's ledger.
     *
     * [seriesId] is client-generated so a create retried after a dropped response resolves to the
     * same schedule rather than a second one quietly writing the same amount every month.
     */
    private suspend fun saveSeries(
        current: AddEntryState,
        title: String,
        description: String,
        amount: Double,
        splits: List<NewTabEntrySplit>,
        exchangeRate: Double?,
    ) {
        val repeat = current.repeat ?: return
        val template =
            RecurringTemplate(
                entryType =
                    when (current.entryKind) {
                        EntryKind.EXPENSE -> RecurringEntryType.EXPENSE
                        EntryKind.INCOME -> RecurringEntryType.INCOME
                        EntryKind.SETTLEMENT -> RecurringEntryType.SETTLEMENT
                    },
                title = title,
                description = description,
                amount = amount,
                currencyCode = current.entryCurrencyCode,
                exchangeRate = exchangeRate,
                paidByUserId = current.paidByUserId,
                receivedByUserId = current.receivedByUserId.takeIf { current.isSettlement },
                // The server stores the resolved amount alongside the rule so every occurrence
                // copies identical shares, rather than re-resolving a percentage against a total
                // that could drift. Same resolver the one-off path uses, so the two cannot diverge.
                splits =
                    splits.zip(SplitResolver.resolveAmounts(splits, amount)) { split, resolved ->
                        NewRecurringTemplateSplit(
                            participantId = split.participantId,
                            splitType = split.splitType,
                            value = split.value,
                            resolvedAmount = resolved,
                        )
                    },
                frequency = repeat.frequency,
                interval = repeat.interval,
                startDate = repeat.startDate,
                end = repeat.end,
            )

        val seriesId = current.editingSeriesId
        val result =
            if (seriesId != null) {
                // An edit has to anchor on an occurrence the current schedule actually produces;
                // the picker only offers such dates, and the template starts on the same one.
                val effectiveFrom = current.effectiveFrom
                if (effectiveFrom == null) {
                    _state.update { it.copy(isSubmitting = false) }
                    emitError(UiText.Resource(Res.string.add_entry_error_effective_from_required))
                    return
                }
                recurringSeriesRepository.updateSeries(
                    seriesId = seriesId,
                    effectiveFrom = effectiveFrom,
                    template = template.copy(startDate = effectiveFrom),
                )
            } else {
                recurringSeriesRepository.createSeries(
                    seriesId = generateSeriesId(),
                    groupId = current.groupId,
                    template = template,
                )
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

    /**
     * The id a new schedule is created under, minted here so a retried create cannot produce a
     * second schedule — the server treats it as the idempotency key.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateSeriesId(): String = Uuid.random().toString()

    /**
     * Today as the scheduler counts it.
     *
     * UTC, not the device's zone: this clamps a schedule's start date and anchors the occurrences an
     * edit may take effect from, and the server measures both against its own UTC day. West of UTC
     * the local date runs a day behind, which let the form offer a start date the server then
     * rejected as being in the past.
     */
    private fun todayUtc(): LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .date

    /** The split fields the form seeds its rows from, whichever of entry or template they came from. */
    private data class SplitSeed(
        val participantId: String,
        val splitType: SplitType,
        val value: Double,
    )

    private fun TabEntrySplit.toSeed() = SplitSeed(participantId, splitType, value)

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
        /** Upcoming occurrences offered as the anchor for a "this and future" edit. */
        const val EFFECTIVE_FROM_OPTION_COUNT = 6

        private const val MAX_TITLE_LENGTH = 255
        private const val MAX_DESCRIPTION_LENGTH = 255
    }
}
