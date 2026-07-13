package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.presentation.util.UiText

data class ChangeEmailState(
    val isSubmitting: Boolean = false,
    val isPasswordVisible: Boolean = false,
)

sealed interface ChangeEmailEvent {
    // Carries the formatted "verification sent" message; the email itself only
    // changes server-side once the user confirms the verification link.
    data class Saved(val message: UiText) : ChangeEmailEvent

    data class Error(val message: UiText) : ChangeEmailEvent
}
