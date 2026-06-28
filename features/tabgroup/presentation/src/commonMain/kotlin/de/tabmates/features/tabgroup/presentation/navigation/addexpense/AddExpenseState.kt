package de.tabmates.features.tabgroup.presentation.navigation.addexpense

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class AddExpenseState(
    val groupId: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    // The currency the expense is paid in (defaults to the group's base currency). Amount + splits
    // are entered and displayed in this currency.
    val expenseCurrencyCode: String = "",
    val expenseCurrencySymbol: String = "",
    val expenseCurrencyDecimalDigits: Int = 2,
    // The group's base currency, used to show an approximate converted total when they differ.
    val baseCurrencyCode: String = "",
    val baseCurrencySymbol: String = "",
    val baseCurrencyDecimalDigits: Int = 2,
    val supportedCurrencies: List<Currency> = emptyList(),
    val ratesByCurrency: Map<String, Double> = emptyMap(),
    val currencyQueryState: TextFieldState = TextFieldState(),
    val isCurrencyPickerVisible: Boolean = false,
    val members: List<GroupParticipant> = emptyList(),
    val currentUserId: String = "",
    val amountTextState: TextFieldState = TextFieldState(),
    val titleTextState: TextFieldState = TextFieldState(),
    val descriptionTextState: TextFieldState = TextFieldState(),
    val paidByUserId: String = "",
    val splitType: SplitType = SplitType.EQUAL,
    val splitInputs: List<ParticipantSplitInput> = emptyList(),
    val entryDate: LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .date,
    val isPaidByPickerVisible: Boolean = false,
    val isSplitEditorVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
) {
    /** True when the chosen expense currency differs from the group's base currency. */
    val isForeignCurrency: Boolean
        get() = baseCurrencyCode.isNotEmpty() && expenseCurrencyCode != baseCurrencyCode
}

data class ParticipantSplitInput(
    val participantId: String,
    val included: Boolean = true,
    val exactAmountState: TextFieldState = TextFieldState(),
    val percentageState: TextFieldState = TextFieldState(),
    val sharesState: TextFieldState = TextFieldState("1"),
)
