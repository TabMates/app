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
    val placeholders: List<GroupSettingsPlaceholder> = emptyList(),
    val newPlaceholderTextState: TextFieldState = TextFieldState(),
    val isPlaceholderDialogVisible: Boolean = false,
    val isAddingPlaceholder: Boolean = false,
    val isSaving: Boolean = false,
    val isLeaving: Boolean = false,
    val showLeaveDialog: Boolean = false,
)

data class GroupSettingsPlaceholder(
    val id: String,
    val name: String,
) {
    val initial: String get() = name.firstOrNull()?.uppercase().orEmpty()
}

sealed interface GroupSettingsAction {
    data object Save : GroupSettingsAction

    data object AddPlaceholderClick : GroupSettingsAction

    data object PlaceholderDialogConfirm : GroupSettingsAction

    data object PlaceholderDialogDismiss : GroupSettingsAction

    data object RequestLeave : GroupSettingsAction

    data object ConfirmLeave : GroupSettingsAction

    data object DismissLeaveDialog : GroupSettingsAction
}

sealed interface GroupSettingsEvent {
    data object Saved : GroupSettingsEvent

    data object Left : GroupSettingsEvent

    data class Error(val message: UiText) : GroupSettingsEvent
}
