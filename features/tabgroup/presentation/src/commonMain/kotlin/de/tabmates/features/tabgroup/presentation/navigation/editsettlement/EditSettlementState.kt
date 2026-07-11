package de.tabmates.features.tabgroup.presentation.navigation.editsettlement

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class EditSettlementState(
    val settlementId: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val amountTextState: TextFieldState = TextFieldState(),
    val entryDate: LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
    val isDatePickerVisible: Boolean = false,
    // Fixed fields: never rendered as editable, round-tripped into updateSettlement on save.
    val title: String = "",
    val description: String = "",
    val currencyCode: String = "",
    val currencySymbol: String = "",
    val currencyDecimalDigits: Int = 2,
    val paidByUserId: String = "",
    val receivedByUserId: String = "",
    val currentUserId: String = "",
    val membersById: Map<String, GroupParticipant> = emptyMap(),
)
