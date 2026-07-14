package de.tabmates.features.authentication.presentation.login

import de.tabmates.core.presentation.util.UiText

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent

    data class LoginFailure(val error: UiText) : LoginEvent

    data object ResendVerificationEmailSuccess : LoginEvent

    data object ResendVerificationEmailError : LoginEvent
}
