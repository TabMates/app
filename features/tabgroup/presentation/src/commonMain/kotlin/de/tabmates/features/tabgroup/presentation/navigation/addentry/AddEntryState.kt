package de.tabmates.features.tabgroup.presentation.navigation.addentry

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class AddEntryState(
    val groupId: String = "",
    val isEditing: Boolean = false,
    // Expense vs. income. Switchable via the on-screen toggle while creating; fixed to the loaded
    // entry's kind while editing. Drives which repository method is called and a few labels.
    val entryKind: EntryKind = EntryKind.EXPENSE,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    // The currency the expense is paid in (defaults to the group's base currency). Amount + splits
    // are entered and displayed in this currency.
    val entryCurrencyCode: String = "",
    val entryCurrencySymbol: String = "",
    val entryCurrencyDecimalDigits: Int = 2,
    // The group's base currency, used to show an approximate converted total when they differ.
    val baseCurrencyCode: String = "",
    val baseCurrencySymbol: String = "",
    val baseCurrencyDecimalDigits: Int = 2,
    val supportedCurrencies: List<Currency> = emptyList(),
    val ratesByCurrency: Map<String, Double> = emptyMap(),
    val ratesLastUpdatedAt: Instant? = null,
    // Edit mode only: currency + locked-in rate as the expense was loaded. A save keeps the
    // original rate unless the currency changed, in which case the current rate is re-snapshotted.
    val originalCurrencyCode: String = "",
    val originalExchangeRate: Double? = null,
    val currencyQueryState: TextFieldState = TextFieldState(),
    val isCurrencyPickerVisible: Boolean = false,
    /** Active members only: who may be picked as payer, and who a *new* entry can be split across. */
    val members: List<GroupParticipant> = emptyList(),
    /**
     * Everyone this screen has to be able to name: [members] plus anyone the edited entry already
     * references — payer or split — who has since been removed from the group.
     */
    val participantsById: Map<String, GroupParticipant> = emptyMap(),
    /** Subset of [participantsById] that is no longer in the group. */
    val formerParticipantIds: Set<String> = emptySet(),
    val currentUserId: String = "",
    val amountTextState: TextFieldState = TextFieldState(),
    val titleTextState: TextFieldState = TextFieldState(),
    val descriptionTextState: TextFieldState = TextFieldState(),
    val paidByUserId: String = "",
    /** Settlements only: who received the money. Must differ from [paidByUserId]. */
    val receivedByUserId: String = "",
    val splitType: SplitType = SplitType.EQUAL,
    val splitInputs: List<ParticipantSplitInput> = emptyList(),
    val entryDate: LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
    val isPaidByPickerVisible: Boolean = false,
    val isReceivedByPickerVisible: Boolean = false,
    val isSplitEditorVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    // --- repeat editor ---
    // Held as separate fields rather than a nullable RepeatConfig so the editor keeps the interval,
    // start date and end rule while the user flips through "Never" and back. [repeat] assembles
    // them, and is null exactly when the entry does not repeat.
    val repeatFrequency: RecurrenceFrequency? = null,
    val repeatInterval: Int = 1,
    val repeatStartDate: LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
    val repeatEnd: RecurringEnd = RecurringEnd.Never,
    val isRepeatEditorVisible: Boolean = false,
    val isRepeatStartPickerVisible: Boolean = false,
    val isRepeatEndPickerVisible: Boolean = false,
    /**
     * Schedule management needs a connection. Unlike an entry, a schedule is a standing instruction
     * to write into other people's ledgers, so it is not queued offline — the repeat controls are
     * disabled instead of silently deferring.
     */
    val isOnline: Boolean = true,
    /** Set when the screen is editing a schedule rather than an entry. */
    val editingSeriesId: String? = null,
    /**
     * The occurrence a schedule edit takes effect from. The server only accepts a future date the
     * current schedule actually produces, so these are offered as a list rather than a free picker.
     */
    val effectiveFromOptions: List<LocalDate> = emptyList(),
    val effectiveFrom: LocalDate? = null,
    val isEffectiveFromPickerVisible: Boolean = false,
) {
    /**
     * How the entry repeats, or null for a one-off. Non-null makes the form save a recurring
     * schedule *instead of* an entry — the server writes the first occurrence itself, so saving both
     * would book the same thing twice.
     */
    val repeat: RepeatConfig?
        get() =
            repeatFrequency?.let { frequency ->
                RepeatConfig(
                    frequency = frequency,
                    interval = repeatInterval,
                    startDate = repeatStartDate,
                    end = repeatEnd,
                )
            }

    /** True when the chosen expense currency differs from the group's base currency. */
    val isForeignCurrency: Boolean
        get() = baseCurrencyCode.isNotEmpty() && entryCurrencyCode != baseCurrencyCode

    val isSettlement: Boolean
        get() = entryKind == EntryKind.SETTLEMENT

    val isEditingSeries: Boolean
        get() = editingSeriesId != null

    /** Splits only exist for expenses and incomes; a settlement moves a fixed amount one way. */
    val hasSplits: Boolean
        get() = !isSettlement

    /**
     * Whether the repeat controls can be touched. Editing an existing one-off entry cannot turn it
     * into a schedule — the entry is already written, and the server has no path that converts one.
     */
    val canEditRepeat: Boolean
        get() = isOnline && (!isEditing || isEditingSeries)
}

data class ParticipantSplitInput(
    val participantId: String,
    val included: Boolean = true,
    val exactAmountState: TextFieldState = TextFieldState(),
    val percentageState: TextFieldState = TextFieldState(),
    val sharesState: TextFieldState = TextFieldState("1"),
)
