package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.presentation.util.UiText

data class MigrateAccountState(
    val isSubmitting: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
)

sealed interface MigrateAccountEvent {
    data class Migrated(val email: String) : MigrateAccountEvent

    data class Error(val message: UiText) : MigrateAccountEvent
}
