package de.tabmates.features.authentication.presentation.login

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.core.presentation.util.UiText

data class LoginState(
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val passwordTextFieldState: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false,
    val canLogin: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isEmailNotVerified: Boolean = false,
    val isResendingVerificationEmail: Boolean = false,
    val resendVerificationError: UiText? = null,
)
