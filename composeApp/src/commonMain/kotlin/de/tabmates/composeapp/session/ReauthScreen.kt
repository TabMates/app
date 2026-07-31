package de.tabmates.composeapp.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.authentication.presentation.navigation.ReauthForgotPassword
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.reauth_desc_email_changed
import tabmatesapp.composeapp.generated.resources.reauth_desc_guest
import tabmatesapp.composeapp.generated.resources.reauth_desc_registered
import tabmatesapp.composeapp.generated.resources.reauth_dialog_confirm
import tabmatesapp.composeapp.generated.resources.reauth_dialog_dismiss
import tabmatesapp.composeapp.generated.resources.reauth_dialog_text_none
import tabmatesapp.composeapp.generated.resources.reauth_dialog_text_one
import tabmatesapp.composeapp.generated.resources.reauth_dialog_text_other
import tabmatesapp.composeapp.generated.resources.reauth_dialog_title
import tabmatesapp.composeapp.generated.resources.reauth_email_hint
import tabmatesapp.composeapp.generated.resources.reauth_email_locked_hint
import tabmatesapp.composeapp.generated.resources.reauth_forgot_password
import tabmatesapp.composeapp.generated.resources.reauth_password_hint
import tabmatesapp.composeapp.generated.resources.reauth_sign_in
import tabmatesapp.composeapp.generated.resources.reauth_start_over
import tabmatesapp.composeapp.generated.resources.reauth_switch_account
import tabmatesapp.composeapp.generated.resources.reauth_title
import tabmatesapp.composeapp.generated.resources.reauth_unsynced_one
import tabmatesapp.composeapp.generated.resources.reauth_unsynced_other

@Composable
fun ReauthRoot(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    viewModel: ReauthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ReauthEvent.ReauthFailed -> snackbarHostState.showSnackbar(event.error.asStringAsync())
            // Back to wherever the banner was tapped from, with the session restored. Switching
            // accounts needs no branch here: it leaves no session at all, and the shell routes
            // that to the welcome screen on its own.
            ReauthEvent.ReauthSucceeded -> backStack.removeLastOrNull()
        }
    }

    ReauthScreen(
        state = state,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSignInClick = viewModel::onSignIn,
        onForgotPasswordClick = { backStack.add(ReauthForgotPassword) },
        onSwitchAccountClick = viewModel::onSwitchAccountClick,
        onDismissSwitchAccountDialog = viewModel::onDismissSwitchAccountDialog,
        onConfirmSwitchAccount = viewModel::onConfirmSwitchAccount,
    )
}

@Composable
private fun ReauthScreen(
    state: ReauthState,
    onTogglePasswordVisibility: () -> Unit,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSwitchAccountClick: () -> Unit,
    onDismissSwitchAccountDialog: () -> Unit,
    onConfirmSwitchAccount: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.reauth_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text =
                when {
                    state.isGuest -> stringResource(Res.string.reauth_desc_guest)
                    state.isEmailLocked -> stringResource(Res.string.reauth_desc_registered)
                    else -> stringResource(Res.string.reauth_desc_email_changed)
                },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp),
        )
        if (state.pendingWriteCount > 0) {
            Text(
                text = unsyncedText(state.pendingWriteCount),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp),
            )
        }

        if (!state.isGuest) {
            TabMatesTextField(
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
                state = state.emailTextFieldState,
                placeholder = stringResource(Res.string.reauth_email_hint),
                supportingText =
                    stringResource(Res.string.reauth_email_locked_hint).takeIf { state.isEmailLocked },
                enabled = !state.isEmailLocked,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                contentType = ContentType.EmailAddress,
            )
            TabMatesPasswordTextField(
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
                state = state.passwordTextFieldState,
                placeholder = stringResource(Res.string.reauth_password_hint),
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibilityClick = onTogglePasswordVisibility,
                imeAction = ImeAction.Go,
                onKeyboardAction = {
                    if (state.canSubmit) {
                        focusManager.clearFocus()
                        onSignInClick()
                    }
                },
            )
            TabMatesInlineLinkText(
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
                textBeforeLink = "",
                linkText = stringResource(Res.string.reauth_forgot_password),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                onLinkClick = onForgotPasswordClick,
            )
            VerticalSpacer(4.dp)
            TabMatesButton(
                modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth(),
                text = stringResource(Res.string.reauth_sign_in),
                onClick = onSignInClick,
                enabled = state.canSubmit,
                isLoading = state.isSigningIn,
            )
        }

        TabMatesButton(
            modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth(),
            text =
                if (state.isGuest) {
                    stringResource(Res.string.reauth_start_over)
                } else {
                    stringResource(Res.string.reauth_switch_account)
                },
            onClick = onSwitchAccountClick,
            style = TabMatesButtonStyle.Secondary,
        )
    }

    if (state.showSwitchAccountDialog) {
        AlertDialog(
            onDismissRequest = onDismissSwitchAccountDialog,
            title = { Text(text = stringResource(Res.string.reauth_dialog_title)) },
            text = { Text(text = switchAccountDialogText(state.pendingWriteCount)) },
            confirmButton = {
                TextButton(onClick = onConfirmSwitchAccount) {
                    Text(text = stringResource(Res.string.reauth_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSwitchAccountDialog) {
                    Text(text = stringResource(Res.string.reauth_dialog_dismiss))
                }
            },
        )
    }
}

@Composable
private fun unsyncedText(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.reauth_unsynced_one)
    } else {
        stringResource(Res.string.reauth_unsynced_other, count)
    }

@Composable
private fun switchAccountDialogText(count: Int): String =
    when (count) {
        0 -> stringResource(Res.string.reauth_dialog_text_none)
        1 -> stringResource(Res.string.reauth_dialog_text_one)
        else -> stringResource(Res.string.reauth_dialog_text_other, count)
    }

@PreviewThemes
@Composable
private fun ReauthScreenPreview() {
    TabMatesTheme {
        Surface {
            ReauthScreen(
                state =
                    ReauthState(
                        emailTextFieldState = TextFieldState("lena@example.com"),
                        isEmailLocked = true,
                        pendingWriteCount = 3,
                    ),
                onTogglePasswordVisibility = {},
                onSignInClick = {},
                onForgotPasswordClick = {},
                onSwitchAccountClick = {},
                onDismissSwitchAccountDialog = {},
                onConfirmSwitchAccount = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun ReauthScreenGuestPreview() {
    TabMatesTheme {
        Surface {
            ReauthScreen(
                state = ReauthState(isGuest = true, pendingWriteCount = 1),
                onTogglePasswordVisibility = {},
                onSignInClick = {},
                onForgotPasswordClick = {},
                onSwitchAccountClick = {},
                onDismissSwitchAccountDialog = {},
                onConfirmSwitchAccount = {},
            )
        }
    }
}
