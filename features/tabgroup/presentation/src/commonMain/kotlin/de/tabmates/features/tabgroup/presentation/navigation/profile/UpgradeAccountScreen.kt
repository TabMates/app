package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.EmailInputTransformation
import de.tabmates.core.designsystem.textfields.TabMatesPasswordTextField
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_confirm_password_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_email_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_intro
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_password_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_password_requirements
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_pending_desc
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_pending_hint
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_pending_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_resend
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_sent
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_submit
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_use_different_email

@Composable
fun UpgradeAccountRoot(
    snackbarHostState: SnackbarHostState,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpgradeAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            // The screen itself switches to the pending phase; the snackbar confirms the send,
            // which is the only feedback a resend produces.
            is UpgradeAccountEvent.VerificationSent -> {
                snackbarHostState.showSnackbar(getString(Res.string.upgrade_account_sent, event.email))
            }

            is UpgradeAccountEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }

            UpgradeAccountEvent.AlreadyRegistered -> {
                onCompleted()
            }
        }
    }

    UpgradeAccountScreen(
        state = state,
        emailState = viewModel.emailState,
        passwordState = viewModel.passwordState,
        confirmPasswordState = viewModel.confirmPasswordState,
        onEmailFocusChanged = { hasFocus -> if (!hasFocus) viewModel.validateEmailOnBlur() },
        onPasswordFocusChanged = { hasFocus -> if (!hasFocus) viewModel.validatePasswordOnBlur() },
        onConfirmPasswordFocusChanged = { hasFocus ->
            if (!hasFocus) viewModel.validateConfirmPasswordOnBlur()
        },
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::onToggleConfirmPasswordVisibility,
        onSubmit = viewModel::onSubmit,
        onResend = viewModel::onResend,
        onUseDifferentEmail = viewModel::onUseDifferentEmail,
        modifier = modifier,
    )
}

@Composable
private fun UpgradeAccountScreen(
    state: UpgradeAccountState,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    confirmPasswordState: TextFieldState,
    onEmailFocusChanged: (Boolean) -> Unit,
    onPasswordFocusChanged: (Boolean) -> Unit,
    onConfirmPasswordFocusChanged: (Boolean) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onUseDifferentEmail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountFieldColumn(modifier = modifier) {
        val pendingEmail = state.pendingEmail
        if (pendingEmail != null) {
            PendingVerification(
                email = pendingEmail,
                isSubmitting = state.isSubmitting,
                onResend = onResend,
                onUseDifferentEmail = onUseDifferentEmail,
            )
        } else {
            UpgradeAccountForm(
                state = state,
                emailState = emailState,
                passwordState = passwordState,
                confirmPasswordState = confirmPasswordState,
                onEmailFocusChanged = onEmailFocusChanged,
                onPasswordFocusChanged = onPasswordFocusChanged,
                onConfirmPasswordFocusChanged = onConfirmPasswordFocusChanged,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onToggleConfirmPasswordVisibility = onToggleConfirmPasswordVisibility,
                onSubmit = onSubmit,
            )
        }
    }
}

@Composable
private fun UpgradeAccountForm(
    state: UpgradeAccountState,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    confirmPasswordState: TextFieldState,
    onEmailFocusChanged: (Boolean) -> Unit,
    onPasswordFocusChanged: (Boolean) -> Unit,
    onConfirmPasswordFocusChanged: (Boolean) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Text(
        text = stringResource(Res.string.upgrade_account_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    VerticalSpacer(16.dp)
    TabMatesTextField(
        state = emailState,
        title = stringResource(Res.string.upgrade_account_email_label),
        supportingText = state.emailError?.asString().orEmpty(),
        isError = state.emailError != null,
        singleLine = true,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
        capitalization = KeyboardCapitalization.None,
        contentType = ContentType.EmailAddress,
        inputTransformation = EmailInputTransformation,
        onFocusChanged = onEmailFocusChanged,
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(16.dp)
    TabMatesPasswordTextField(
        state = passwordState,
        title = stringResource(Res.string.upgrade_account_password_label),
        supportingText =
            state.passwordError?.asString()
                ?: stringResource(Res.string.upgrade_account_password_requirements),
        isError = state.passwordError != null,
        isPasswordVisible = state.isPasswordVisible,
        onToggleVisibilityClick = onTogglePasswordVisibility,
        imeAction = ImeAction.Next,
        onFocusChanged = onPasswordFocusChanged,
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(16.dp)
    TabMatesPasswordTextField(
        state = confirmPasswordState,
        title = stringResource(Res.string.upgrade_account_confirm_password_label),
        supportingText = state.confirmPasswordError?.asString().orEmpty(),
        isError = state.confirmPasswordError != null,
        isPasswordVisible = state.isConfirmPasswordVisible,
        onToggleVisibilityClick = onToggleConfirmPasswordVisibility,
        imeAction = ImeAction.Done,
        onFocusChanged = onConfirmPasswordFocusChanged,
        onKeyboardAction = {
            if (!state.isSubmitting) {
                focusManager.clearFocus()
                onSubmit()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(24.dp)
    TabMatesButton(
        text = stringResource(Res.string.upgrade_account_submit),
        onClick = onSubmit,
        enabled = !state.isSubmitting,
        isLoading = state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PendingVerification(
    email: String,
    isSubmitting: Boolean,
    onResend: () -> Unit,
    onUseDifferentEmail: () -> Unit,
) {
    Text(
        text = stringResource(Res.string.upgrade_account_pending_title),
        style = MaterialTheme.typography.titleMedium,
    )
    VerticalSpacer(8.dp)
    Text(
        text = stringResource(Res.string.upgrade_account_pending_desc, email),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    VerticalSpacer(8.dp)
    Text(
        text = stringResource(Res.string.upgrade_account_pending_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    VerticalSpacer(24.dp)
    TabMatesButton(
        text = stringResource(Res.string.upgrade_account_resend),
        onClick = onResend,
        enabled = !isSubmitting,
        isLoading = isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(8.dp)
    TabMatesButton(
        text = stringResource(Res.string.upgrade_account_use_different_email),
        onClick = onUseDifferentEmail,
        style = TabMatesButtonStyle.Secondary,
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}
