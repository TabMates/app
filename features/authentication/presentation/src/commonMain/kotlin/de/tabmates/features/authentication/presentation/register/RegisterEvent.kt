package de.tabmates.features.authentication.presentation.register

sealed interface RegisterEvent {
    data class Success(val email: String) : RegisterEvent
}
