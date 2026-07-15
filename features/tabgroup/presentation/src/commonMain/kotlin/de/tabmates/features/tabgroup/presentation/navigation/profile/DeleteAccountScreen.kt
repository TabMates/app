package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesPasswordTextField
import de.tabmates.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_button
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_dialog_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_password_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_warning

@Composable
fun DeleteAccountRoot(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            // Session is cleared on delete; the app shell observes it and returns to Welcome.
            DeleteAccountEvent.Deleted -> Unit

            is DeleteAccountEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    DeleteAccountScreen(
        state = state,
        passwordState = viewModel.passwordState,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onDeleteClick = viewModel::onDeleteClick,
        onConfirmDelete = viewModel::onConfirmDelete,
        onDismissDialog = viewModel::onDismissDialog,
        modifier = modifier,
    )
}

@Composable
private fun DeleteAccountScreen(
    state: DeleteAccountState,
    passwordState: TextFieldState,
    onTogglePasswordVisibility: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountFieldColumn(modifier = modifier) {
        Text(
            text = stringResource(Res.string.delete_account_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.isRegistered) {
            VerticalSpacer(16.dp)
            TabMatesPasswordTextField(
                state = passwordState,
                title = stringResource(Res.string.delete_account_password_label),
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibilityClick = onTogglePasswordVisibility,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        VerticalSpacer(24.dp)
        TabMatesButton(
            text = stringResource(Res.string.delete_account_button),
            onClick = onDeleteClick,
            style = TabMatesButtonStyle.DestructivePrimary,
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(stringResource(Res.string.delete_account_dialog_title)) },
            text = { Text(stringResource(Res.string.delete_account_dialog_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(stringResource(Res.string.delete_account_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) {
                    Text(stringResource(Res.string.delete_account_dialog_cancel))
                }
            },
        )
    }
}
