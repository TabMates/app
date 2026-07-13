package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.presentation.util.UiText

data class AccountEditState(
    val isSubmitting: Boolean = false,
)

data class ChangePasswordState(
    val isSubmitting: Boolean = false,
    val isCurrentPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
)

sealed interface AccountEditEvent {
    data object Saved : AccountEditEvent

    data class Error(val message: UiText) : AccountEditEvent
}
