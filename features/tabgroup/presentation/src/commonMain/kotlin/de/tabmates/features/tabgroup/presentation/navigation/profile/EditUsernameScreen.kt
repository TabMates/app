package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.account_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.account_saved
import tabmatesapp.features.tabgroup.presentation.generated.resources.edit_username_label

@Composable
fun EditUsernameRoot(
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditUsernameViewModel = koinViewModel(),
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
        TabMatesTextField(
            state = viewModel.usernameState,
            title = stringResource(Res.string.edit_username_label),
            singleLine = true,
            capitalization = KeyboardCapitalization.Words,
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

@Composable
internal fun AccountFieldColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            VerticalSpacer(8.dp)
            content()
        }
    }
}
