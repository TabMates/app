package de.tabmates.features.authentication.presentation.registerguest

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.core.presentation.util.UiText

data class RegisterGuestState(
    val usernameTextState: TextFieldState = TextFieldState(),
    val usernameError: UiText? = null,
    val isRegistering: Boolean = false,
)
