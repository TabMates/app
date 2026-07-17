package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesPasswordTextField
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.account_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_new_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_password_label

@Composable
fun ChangeEmailRoot(
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangeEmailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ChangeEmailEvent.Saved -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
                onSaved()
            }

            is ChangeEmailEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    TopBarActions(navKey) {
        TextButton(onClick = viewModel::onSave, enabled = !state.isSubmitting) {
            Text(text = stringResource(Res.string.account_save), fontWeight = FontWeight.SemiBold)
        }
    }

    val focusManager = LocalFocusManager.current
    AccountFieldColumn(modifier = modifier) {
        TabMatesTextField(
            state = viewModel.newEmailState,
            title = stringResource(Res.string.change_email_new_label),
            singleLine = true,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TabMatesPasswordTextField(
            state = viewModel.passwordState,
            title = stringResource(Res.string.change_email_password_label),
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibilityClick = viewModel::onTogglePasswordVisibility,
            imeAction = ImeAction.Done,
            onKeyboardAction = {
                if (!state.isSubmitting) {
                    focusManager.clearFocus()
                    viewModel.onSave()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
