package de.tabmates.composeapp.session

import androidx.compose.foundation.text.input.TextFieldState

data class ReauthState(
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val passwordTextFieldState: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false,
    /**
     * True when the address is known to still be the account's. Locking it is what guarantees the
     * unsynced writes go back to the account that made them.
     */
    val isEmailLocked: Boolean = false,
    /** Guest accounts have no credentials, so there is nothing to sign back in with. */
    val isGuest: Boolean = false,
    val canSubmit: Boolean = false,
    val isSigningIn: Boolean = false,
    val pendingWriteCount: Int = 0,
    val showSwitchAccountDialog: Boolean = false,
)
