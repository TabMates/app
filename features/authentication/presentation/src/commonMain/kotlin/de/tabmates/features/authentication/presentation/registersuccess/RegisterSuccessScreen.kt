package de.tabmates.features.authentication.presentation.registersuccess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.authentication.presentation.navigation.Login
import de.tabmates.features.authentication.presentation.navigation.Welcome
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.account_successfully_created
import tabmatesapp.features.authentication.presentation.generated.resources.login
import tabmatesapp.features.authentication.presentation.generated.resources.resend_verification_email
import tabmatesapp.features.authentication.presentation.generated.resources.resent_verification_email

@Composable
fun RegisterSuccessRoot(
    backStack: NavBackStack<NavKey>,
    email: String,
    snackbarHostState: SnackbarHostState,
    registerSuccessViewModel: RegisterSuccessViewModel = koinViewModel(parameters = { parametersOf(email) }),
    modifier: Modifier = Modifier,
) {
    val state by registerSuccessViewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(registerSuccessViewModel.events) { event ->
        when (event) {
            is RegisterSuccessEvent.ResendVerificationEmailSuccess -> {
                snackbarHostState.showSnackbar(getString(Res.string.resent_verification_email))
            }

            is RegisterSuccessEvent.ResendVerificationEmailError -> {
                snackbarHostState.showSnackbar(state.resendVerificationError?.asStringAsync().orEmpty())
            }
        }
    }

    RegisterSuccessScreen(
        isResendingVerificationEmail = state.isResendingVerificationEmail,
        modifier = modifier,
        onLoginClick = {
            backStack.clear()
            backStack.add(Welcome)
            backStack.add(Login)
        },
        onResentEmailVerification = registerSuccessViewModel::resendVerification,
    )
}

@Composable
private fun RegisterSuccessScreen(
    isResendingVerificationEmail: Boolean,
    onLoginClick: () -> Unit,
    onResentEmailVerification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(Res.string.account_successfully_created),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            text = stringResource(Res.string.login),
            onClick = onLoginClick,
        )
        VerticalSpacer(8.dp)
        TabMatesButton(
            text = stringResource(Res.string.resend_verification_email),
            onClick = onResentEmailVerification,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            enabled = !isResendingVerificationEmail,
            isLoading = isResendingVerificationEmail,
            style = TabMatesButtonStyle.Secondary,
        )
    }
}

@PreviewThemes
@Composable
private fun RegisterSuccessScreenPreview() {
    TabMatesTheme {
        Surface {
            RegisterSuccessScreen(
                onLoginClick = {},
                onResentEmailVerification = {},
                modifier = Modifier.fillMaxSize(),
                isResendingVerificationEmail = false,
            )
        }
    }
}

@PreviewThemes
@Composable
private fun RegisterSuccessScreenPreview2() {
    TabMatesTheme {
        Surface {
            RegisterSuccessScreen(
                onLoginClick = {},
                onResentEmailVerification = {},
                modifier = Modifier.fillMaxSize(),
                isResendingVerificationEmail = true,
            )
        }
    }
}
