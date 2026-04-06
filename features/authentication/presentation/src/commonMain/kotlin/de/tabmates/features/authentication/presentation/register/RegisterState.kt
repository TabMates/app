package de.tabmates.features.authentication.presentation.register

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.core.presentation.util.UiText

data class RegisterState(
    val emailTextState: TextFieldState = TextFieldState(),
    val emailError: UiText? = null,
    val passwordTextState: TextFieldState = TextFieldState(),
    val passwordError: UiText? = null,
    val usernameTextState: TextFieldState = TextFieldState(),
    val usernameError: UiText? = null,
    val isRegistering: Boolean = false,
    val isPasswordVisible: Boolean = false,
)
