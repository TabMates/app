package de.tabmates.features.authentication.presentation.registersuccess

sealed interface RegisterSuccessEvent {
    data object ResendVerificationEmailSuccess : RegisterSuccessEvent

    data object ResendVerificationEmailError : RegisterSuccessEvent
}
