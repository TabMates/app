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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesPasswordTextField
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.account_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.account_saved
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_password_current_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_password_new_label

@Composable
fun ChangePasswordRoot(
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AccountEditEvent.Saved -> {
                snackbarHostState.showSnackbar(getString(Res.string.account_saved))
                onSaved()
            }

            is AccountEditEvent.Error -> {
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
        TabMatesPasswordTextField(
            state = viewModel.currentPasswordState,
            title = stringResource(Res.string.change_password_current_label),
            isPasswordVisible = state.isCurrentPasswordVisible,
            onToggleVisibilityClick = viewModel::onToggleCurrentPasswordVisibility,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TabMatesPasswordTextField(
            state = viewModel.newPasswordState,
            title = stringResource(Res.string.change_password_new_label),
            isPasswordVisible = state.isNewPasswordVisible,
            onToggleVisibilityClick = viewModel::onToggleNewPasswordVisibility,
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
