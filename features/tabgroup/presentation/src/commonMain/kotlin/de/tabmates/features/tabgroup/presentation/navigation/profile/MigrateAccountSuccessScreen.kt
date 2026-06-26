package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_done
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_email_sent_to_x
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_resend
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_resent
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_success_title

@Composable
fun MigrateAccountSuccessRoot(
    email: String,
    snackbarHostState: SnackbarHostState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MigrateAccountSuccessViewModel = koinViewModel(parameters = { parametersOf(email) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            MigrateAccountSuccessEvent.ResendSuccess -> {
                snackbarHostState.showSnackbar(getString(Res.string.migrate_account_resent))
            }

            is MigrateAccountSuccessEvent.ResendError -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    MigrateAccountSuccessScreen(
        email = state.email,
        isResending = state.isResending,
        onResendClick = viewModel::resendVerification,
        onDoneClick = onDone,
        modifier = modifier,
    )
}

@Composable
private fun MigrateAccountSuccessScreen(
    email: String,
    isResending: Boolean,
    onResendClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.migrate_account_success_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(16.dp)
        Text(
            text = stringResource(Res.string.migrate_account_email_sent_to_x, email),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(24.dp)
        TabMatesButton(
            text = stringResource(Res.string.migrate_account_done),
            onClick = onDoneClick,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        )
        VerticalSpacer(8.dp)
        TabMatesButton(
            text = stringResource(Res.string.migrate_account_resend),
            onClick = onResendClick,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            enabled = !isResending,
            isLoading = isResending,
            style = TabMatesButtonStyle.Secondary,
        )
    }
}
