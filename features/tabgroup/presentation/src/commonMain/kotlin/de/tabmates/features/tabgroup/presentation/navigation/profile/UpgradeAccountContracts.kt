package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.presentation.util.UiText

/**
 * @param pendingEmail the address a verification link was sent to, or `null` while the form is
 *   still being filled in. It drives the two phases of the screen: the account is untouched until
 *   that link is redeemed, so the guest session keeps working the whole time.
 */
data class UpgradeAccountState(
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val isSubmitting: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val pendingEmail: String? = null,
)

sealed interface UpgradeAccountEvent {
    data class VerificationSent(val email: String) : UpgradeAccountEvent

    data class Error(val message: UiText) : UpgradeAccountEvent

    /** The link was redeemed elsewhere while this screen was open — there is nothing left to do. */
    data object AlreadyRegistered : UpgradeAccountEvent
}
