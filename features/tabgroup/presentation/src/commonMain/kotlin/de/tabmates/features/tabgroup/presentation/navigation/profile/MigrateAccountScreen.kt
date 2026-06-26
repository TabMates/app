package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_confirm_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_email_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_password_label

@Composable
fun MigrateAccountRoot(
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onMigrated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MigrateAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is MigrateAccountEvent.Migrated -> {
                onMigrated(event.email)
            }

            is MigrateAccountEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    TopBarActions(navKey) {
        TextButton(onClick = viewModel::onSave, enabled = !state.isSubmitting) {
            Text(text = stringResource(Res.string.account_save), fontWeight = FontWeight.SemiBold)
        }
    }

    AccountFieldColumn(modifier = modifier) {
        TabMatesTextField(
            state = viewModel.emailState,
            title = stringResource(Res.string.migrate_account_email_label),
            singleLine = true,
            keyboardType = KeyboardType.Email,
            isError = state.emailError != null,
            supportingText = state.emailError?.asString(),
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TabMatesPasswordTextField(
            state = viewModel.passwordState,
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibilityClick = viewModel::togglePasswordVisibility,
            title = stringResource(Res.string.migrate_account_password_label),
            isError = state.passwordError != null,
            supportingText = state.passwordError?.asString(),
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TabMatesPasswordTextField(
            state = viewModel.confirmPasswordState,
            isPasswordVisible = state.isConfirmPasswordVisible,
            onToggleVisibilityClick = viewModel::toggleConfirmPasswordVisibility,
            title = stringResource(Res.string.migrate_account_confirm_label),
            isError = state.confirmPasswordError != null,
            supportingText = state.confirmPasswordError?.asString(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
