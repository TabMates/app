package de.tabmates.features.authentication.presentation.register

import de.tabmates.core.presentation.util.UiText

sealed interface RegisterEvent {
    data class Success(val email: String) : RegisterEvent

    data class RegistrationError(val message: UiText) : RegisterEvent
}
