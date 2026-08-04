package de.tabmates.features.authentication.presentation.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.EmailInputTransformation
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.core.presentation.util.UiText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.forgot_password
import tabmatesapp.features.authentication.presentation.generated.resources.forgot_password_email_sent_successfully
import tabmatesapp.features.authentication.presentation.generated.resources.register_email_hint
import tabmatesapp.features.authentication.presentation.generated.resources.submit

@Composable
fun ForgotPasswordScreenRoot(
    modifier: Modifier = Modifier,
    forgotPasswordViewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by forgotPasswordViewModel.state.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        emailTextFieldState = state.emailTextFieldState,
        errorText = state.errorText,
        onSubmitClick = forgotPasswordViewModel::submitForgotPasswordRequest,
        modifier = modifier,
        enabled = !state.isLoading && state.canSubmit,
        isLoading = state.isLoading,
        isEmailSentSuccessfully = state.isEmailSentSuccessfully,
    )
}

@Composable
private fun ForgotPasswordScreen(
    emailTextFieldState: TextFieldState,
    errorText: UiText?,
    enabled: Boolean,
    isLoading: Boolean,
    isEmailSentSuccessfully: Boolean,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.forgot_password),
            style = MaterialTheme.typography.headlineMedium,
        )
        VerticalSpacer(16.dp)
        TabMatesTextField(
            state = emailTextFieldState,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            placeholder = stringResource(Res.string.register_email_hint),
            keyboardType = KeyboardType.Email,
            singleLine = true,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.None,
            inputTransformation = EmailInputTransformation,
            onKeyboardAction = {
                if (enabled) {
                    focusManager.clearFocus()
                    onSubmitClick()
                }
            },
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            text = stringResource(Res.string.submit),
            onClick = onSubmitClick,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            enabled = enabled,
            isLoading = isLoading,
        )
        if (isEmailSentSuccessfully) {
            VerticalSpacer(16.dp)
            Text(
                stringResource(Res.string.forgot_password_email_sent_successfully),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.extended.positive,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        errorText?.let {
            VerticalSpacer(16.dp)
            Text(
                text = errorText.asString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@PreviewThemes
private fun ForgotPasswordScreenPreview() {
    TabMatesTheme {
        Surface {
            ForgotPasswordScreen(
                emailTextFieldState = TextFieldState(),
                errorText = UiText.DynamicString("This is an error message"),
                onSubmitClick = { },
                enabled = true,
                isLoading = false,
                isEmailSentSuccessfully = true,
            )
        }
    }
}
