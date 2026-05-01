package de.tabmates.features.authentication.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.TabMatesInlineLinkText
import de.tabmates.core.designsystem.textfields.TabMatesPasswordTextField
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.headlineLargeBold
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.authentication.presentation.di.authPresentationModule
import de.tabmates.features.authentication.presentation.navigation.Login
import de.tabmates.features.authentication.presentation.navigation.Register
import de.tabmates.features.authentication.presentation.navigation.RegisterGuest
import de.tabmates.features.authentication.presentation.navigation.RegisterSuccess
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.register_already_have_account_prefix
import tabmatesapp.features.authentication.presentation.generated.resources.register_confirm_password_hint
import tabmatesapp.features.authentication.presentation.generated.resources.register_email_hint
import tabmatesapp.features.authentication.presentation.generated.resources.register_login_action
import tabmatesapp.features.authentication.presentation.generated.resources.register_password_hint
import tabmatesapp.features.authentication.presentation.generated.resources.register_password_requirements
import tabmatesapp.features.authentication.presentation.generated.resources.register_title
import tabmatesapp.features.authentication.presentation.generated.resources.register_username_hint
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_button_guest

@Composable
fun RegisterRoot(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    registerViewModel: RegisterViewModel = koinViewModel(),
) {
    val state by registerViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(registerViewModel.events) { event ->
        when (event) {
            is RegisterEvent.Success -> {
                backStack.add(RegisterSuccess(event.email))
                backStack.remove(Register)
            }

            is RegisterEvent.RegistrationError -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    RegisterScreen(
        state = state,
        onLoginClick = {
            backStack.add(Login)
            backStack.remove(Register)
        },
        onGuestClick = {
            backStack.add(RegisterGuest)
            backStack.remove(Register)
        },
        onCreateAccountClick = { registerViewModel.register() },
        togglePasswordVisibility = registerViewModel::togglePasswordVisibility,
        toggleConfirmPasswordVisibility = registerViewModel::toggleConfirmPasswordVisibility,
        onUsernameFocusChanged = { hasFocus -> if (!hasFocus) registerViewModel.validateUsernameOnBlur() },
        onEmailFocusChanged = { hasFocus -> if (!hasFocus) registerViewModel.validateEmailOnBlur() },
        onPasswordFocusChanged = { hasFocus -> if (!hasFocus) registerViewModel.validatePasswordOnBlur() },
        onConfirmPasswordFocusChanged = { hasFocus ->
            if (!hasFocus) registerViewModel.validateConfirmPasswordOnBlur()
        },
        modifier = modifier,
    )
}

@Composable
private fun RegisterScreen(
    state: RegisterState,
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    togglePasswordVisibility: () -> Unit,
    toggleConfirmPasswordVisibility: () -> Unit,
    onUsernameFocusChanged: (Boolean) -> Unit,
    onEmailFocusChanged: (Boolean) -> Unit,
    onPasswordFocusChanged: (Boolean) -> Unit,
    onConfirmPasswordFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(Res.string.register_title),
            style = MaterialTheme.typography.headlineLargeBold,
        )
        VerticalSpacer(36.dp)
        TabMatesTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.usernameTextState,
            placeholder = stringResource(Res.string.register_username_hint),
            supportingText = state.usernameError?.asString().orEmpty(),
            isError = state.usernameError != null,
            singleLine = true,
            onFocusChanged = onUsernameFocusChanged,
            contentType = ContentType.Username,
        )
        state.usernameError?.let {
            VerticalSpacer(16.dp)
        }
        TabMatesTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.emailTextState,
            placeholder = stringResource(Res.string.register_email_hint),
            supportingText = state.emailError?.asString().orEmpty(),
            isError = state.emailError != null,
            singleLine = true,
            keyboardType = KeyboardType.Email,
            onFocusChanged = onEmailFocusChanged,
            contentType = ContentType.EmailAddress,
        )
        state.emailError?.let {
            VerticalSpacer(16.dp)
        }
        TabMatesPasswordTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.passwordTextState,
            placeholder = stringResource(Res.string.register_password_hint),
            supportingText =
                state.passwordError?.asString() ?: stringResource(Res.string.register_password_requirements),
            isError = state.passwordError != null,
            onToggleVisibilityClick = togglePasswordVisibility,
            isPasswordVisible = state.isPasswordVisible,
            onFocusChanged = onPasswordFocusChanged,
        )
        state.passwordError?.let {
            VerticalSpacer(16.dp)
        }
        TabMatesPasswordTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.confirmPasswordTextState,
            placeholder = stringResource(Res.string.register_confirm_password_hint),
            supportingText = state.confirmPasswordError?.asString().orEmpty(),
            isError = state.confirmPasswordError != null,
            onToggleVisibilityClick = toggleConfirmPasswordVisibility,
            isPasswordVisible = state.isConfirmPasswordVisible,
            onFocusChanged = onConfirmPasswordFocusChanged,
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            modifier =
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(),
            onClick = onCreateAccountClick,
            text = stringResource(Res.string.register_title),
            enabled = !state.isRegistering,
            isLoading = state.isRegistering,
        )
        VerticalSpacer(16.dp)
        TabMatesInlineLinkText(
            textBeforeLink = stringResource(Res.string.register_already_have_account_prefix),
            linkText = stringResource(Res.string.register_login_action),
            onLinkClick = onLoginClick,
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            onClick = onGuestClick,
            style = TabMatesButtonStyle.Text,
            text = stringResource(Res.string.welcome_button_guest),
            modifier =
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(),
        )
    }
}

@PreviewThemes
@Composable
private fun RegisterScreenPreview() {
    KoinApplicationPreview(
        application = {
            modules(authPresentationModule)
        },
    ) {
        TabMatesTheme {
            Surface {
                RegisterScreen(
                    state =
                        RegisterState(
                            usernameTextState = TextFieldState(""),
                            usernameError = null,
                        ),
                    onLoginClick = {},
                    onGuestClick = {},
                    togglePasswordVisibility = {},
                    toggleConfirmPasswordVisibility = {},
                    onCreateAccountClick = {},
                    onUsernameFocusChanged = {},
                    onEmailFocusChanged = {},
                    onPasswordFocusChanged = {},
                    onConfirmPasswordFocusChanged = {},
                )
            }
        }
    }
}
