package de.tabmates.features.authentication.presentation.resetpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesPasswordTextField
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.core.presentation.util.UiText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.new_password
import tabmatesapp.features.authentication.presentation.generated.resources.reset_password_successfully
import tabmatesapp.features.authentication.presentation.generated.resources.set_new_password
import tabmatesapp.features.authentication.presentation.generated.resources.submit

@Composable
fun ResetPasswordScreenRoot(
    token: String,
    resetPasswordViewModel: ResetPasswordViewModel = koinViewModel(parameters = { parametersOf(token) }),
) {
    val state by resetPasswordViewModel.state.collectAsStateWithLifecycle()

    ResetPasswordScreen(
        passwordTextState = state.passwordTextState,
        isPasswordVisible = state.isPasswordVisible,
        enabled = !state.isLoading && state.canSubmit,
        isLoading = state.isLoading,
        isResetSuccessfully = state.isResetSuccessful,
        onToggleVisibilityClick = resetPasswordViewModel::onTogglePasswordVisibilityClick,
        onSubmitClick = resetPasswordViewModel::submitResetPasswordRequest,
        errorText = state.errorText,
    )
}

@Composable
private fun ResetPasswordScreen(
    passwordTextState: TextFieldState,
    isPasswordVisible: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    isResetSuccessfully: Boolean,
    errorText: UiText?,
    onToggleVisibilityClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 32.dp).fillMaxSize(),
    ) {
        Text(
            text = stringResource(Res.string.set_new_password),
            style = MaterialTheme.typography.headlineMedium,
        )
        VerticalSpacer(height = 16.dp)
        TabMatesPasswordTextField(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            state = passwordTextState,
            isPasswordVisible = isPasswordVisible,
            onToggleVisibilityClick = onToggleVisibilityClick,
            placeholder = stringResource(Res.string.new_password),
            imeAction = ImeAction.Done,
            onKeyboardAction = {
                if (enabled) {
                    focusManager.clearFocus()
                    onSubmitClick()
                }
            },
        )
        VerticalSpacer(height = 16.dp)
        TabMatesButton(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            text = stringResource(Res.string.submit),
            onClick = onSubmitClick,
            enabled = enabled,
            isLoading = isLoading,
        )

        if (isResetSuccessfully) {
            VerticalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.reset_password_successfully),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.extended.positive,
            )
        }
        errorText?.let {
            VerticalSpacer(8.dp)
            Text(
                text = errorText.asString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
@PreviewThemes
private fun ResetPasswordScreenPreview() {
    TabMatesTheme {
        Surface {
            ResetPasswordScreen(
                passwordTextState = TextFieldState(""),
                isPasswordVisible = false,
                enabled = true,
                isLoading = false,
                isResetSuccessfully = true,
                onToggleVisibilityClick = {},
                onSubmitClick = {},
                errorText = UiText.DynamicString("This is an error message"),
            )
        }
    }
}
