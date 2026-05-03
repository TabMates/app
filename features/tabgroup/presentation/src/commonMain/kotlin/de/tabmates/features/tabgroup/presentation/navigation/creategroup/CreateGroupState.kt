package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.features.tabgroup.domain.currency.Currency
import de.tabmates.features.tabgroup.domain.models.GroupParticipant

data class CreateGroupState(
    val nameTextState: TextFieldState = TextFieldState(),
    val descriptionTextState: TextFieldState = TextFieldState(),
    val emailTextState: TextFieldState = TextFieldState(),
    val currencyQueryState: TextFieldState = TextFieldState(),
    val defaultCurrencyCode: String = "",
    val supportedCurrencies: List<Currency> = emptyList(),
    val isCurrencyPickerVisible: Boolean = false,
    val iconKey: String? = null,
    val inviteLink: String? = null,
    val members: List<CreateGroupMember> = emptyList(),
    val isSubmitting: Boolean = false,
)

data class CreateGroupMember(
    val participant: GroupParticipant,
    val isSelected: Boolean,
) {
    val id: String get() = participant.userId
    val name: String get() = participant.username
    val initials: String get() = participant.initials
}
