package de.tabmates.features.authentication.presentation.registerguest

import de.tabmates.core.presentation.util.UiText

sealed interface RegisterGuestEvent {
    data object Success : RegisterGuestEvent

    data class RegistrationError(val message: UiText) : RegisterGuestEvent
}
