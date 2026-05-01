package de.tabmates.features.authentication.presentation.registerguest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.core.presentation.util.UiText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.register_guest_desc
import tabmatesapp.features.authentication.presentation.generated.resources.register_username_hint
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_button_guest

@Composable
fun RegisterGuestRoot(
    snackbarHostState: SnackbarHostState,
    onRegisterSuccess: () -> Unit,
    registerGuestViewModel: RegisterGuestViewModel = koinViewModel(),
) {
    val state by registerGuestViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(registerGuestViewModel.events) { event ->
        when (event) {
            is RegisterGuestEvent.Success -> {
                onRegisterSuccess()
            }

            is RegisterGuestEvent.RegistrationError -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    RegisterGuestScreen(
        usernameTextField = state.usernameTextState,
        usernameError = state.usernameError,
        isRegistering = state.isRegistering,
        onGuestClick = registerGuestViewModel::registerGuest,
    )
}

@Composable
private fun RegisterGuestScreen(
    usernameTextField: TextFieldState,
    usernameError: UiText?,
    onGuestClick: () -> Unit,
    isRegistering: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.welcome_button_guest),
            style = MaterialTheme.typography.headlineLarge,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.register_guest_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(24.dp)
        TabMatesTextField(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            state = usernameTextField,
            placeholder = stringResource(Res.string.register_username_hint),
            isError = usernameError != null,
            supportingText = usernameError?.asString(),
            singleLine = true,
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth(),
            text = stringResource(Res.string.welcome_button_guest),
            onClick = onGuestClick,
            isLoading = isRegistering,
        )
    }
}

@PreviewThemes
@Composable
private fun RegisterGuestScreenPreview() {
    TabMatesTheme {
        Surface {
            RegisterGuestScreen(
                usernameTextField = TextFieldState(""),
                usernameError = null,
                onGuestClick = {},
                isRegistering = false,
            )
        }
    }
}

@PreviewThemes
@Composable
private fun RegisterGuestScreenErrorPreview() {
    TabMatesTheme {
        Surface {
            RegisterGuestScreen(
                usernameTextField = TextFieldState("Guest"),
                usernameError = UiText.DynamicString("Username already exists"),
                onGuestClick = {},
                isRegistering = false,
            )
        }
    }
}
