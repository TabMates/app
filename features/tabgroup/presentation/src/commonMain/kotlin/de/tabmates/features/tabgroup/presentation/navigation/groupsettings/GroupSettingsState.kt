package de.tabmates.features.tabgroup.presentation.navigation.groupsettings

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.core.presentation.util.UiText

data class GroupSettingsState(
    val isLoading: Boolean = true,
    val iconKey: String = "",
    val colorKey: String = "",
    val nameTextState: TextFieldState = TextFieldState(),
    val descriptionTextState: TextFieldState = TextFieldState(),
    val defaultCurrencyCode: String = "",
    val isSaving: Boolean = false,
    val isLeaving: Boolean = false,
    val showLeaveDialog: Boolean = false,
)

sealed interface GroupSettingsAction {
    data object Save : GroupSettingsAction

    data object RequestLeave : GroupSettingsAction

    data object ConfirmLeave : GroupSettingsAction

    data object DismissLeaveDialog : GroupSettingsAction
}

sealed interface GroupSettingsEvent {
    data object Saved : GroupSettingsEvent

    data object Left : GroupSettingsEvent

    data class Error(val message: UiText) : GroupSettingsEvent
}
