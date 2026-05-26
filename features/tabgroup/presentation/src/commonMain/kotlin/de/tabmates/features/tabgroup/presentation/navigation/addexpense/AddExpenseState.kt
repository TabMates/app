package de.tabmates.features.tabgroup.presentation.navigation.addexpense

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import kotlin.time.Clock
import kotlin.time.Instant

data class AddExpenseState(
    val groupId: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val groupCurrencyCode: String = "",
    val groupCurrencySymbol: String = "",
    val groupCurrencyDecimalDigits: Int = 2,
    val members: List<GroupParticipant> = emptyList(),
    val currentUserId: String = "",
    val amountTextState: TextFieldState = TextFieldState(),
    val titleTextState: TextFieldState = TextFieldState(),
    val paidByUserId: String = "",
    val splitType: SplitType = SplitType.EQUAL,
    val splitInputs: List<ParticipantSplitInput> = emptyList(),
    val createdAt: Instant = Clock.System.now(),
    val isPaidByPickerVisible: Boolean = false,
    val isSplitEditorVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
)

data class ParticipantSplitInput(
    val participantId: String,
    val included: Boolean = true,
    val exactAmountState: TextFieldState = TextFieldState(),
    val percentageState: TextFieldState = TextFieldState(),
    val sharesState: TextFieldState = TextFieldState("1"),
)
