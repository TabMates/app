package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.presentation.util.UiText

data class DeleteAccountState(
    // Registered users must confirm with their password; anonymous users have none.
    val isRegistered: Boolean = true,
    val isPasswordVisible: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed interface DeleteAccountEvent {
    // The account was deleted and the session cleared; the app shell returns to Welcome.
    data object Deleted : DeleteAccountEvent

    data class Error(val message: UiText) : DeleteAccountEvent
}
