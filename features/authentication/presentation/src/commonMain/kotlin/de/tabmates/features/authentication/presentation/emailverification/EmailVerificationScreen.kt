package de.tabmates.features.authentication.presentation.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.features.authentication.presentation.navigation.Login
import de.tabmates.features.authentication.presentation.navigation.Welcome
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.close
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_continue
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_failed
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_failed_desc
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_successfully
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_successfully_desc
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_upgraded_desc
import tabmatesapp.features.authentication.presentation.generated.resources.login
import tabmatesapp.features.authentication.presentation.generated.resources.verifying_account

/**
 * @param onExitClick Where leaving the screen leads: back into the app when the link was opened on a
 * live session — upgrading a guest keeps them signed in, and clearing the stack would strand a
 * perfectly valid session — and Welcome when there is no session to return to.
 */
@Composable
fun EmailVerificationRoot(
    token: String,
    backStack: NavBackStack<NavKey>,
    onExitClick: () -> Unit,
    emailVerificationViewModel: EmailVerificationViewModel = koinViewModel(parameters = { parametersOf(token) }),
) {
    val state by emailVerificationViewModel.state.collectAsStateWithLifecycle()
    EmailVerificationScreen(
        state = state,
        onLoginClick = {
            backStack.clear()
            backStack.add(Welcome)
            backStack.add(Login)
        },
        onExitClick = onExitClick,
    )
}

@Composable
private fun EmailVerificationScreen(
    state: EmailVerificationState,
    onLoginClick: () -> Unit,
    onExitClick: () -> Unit,
) {
    when {
        state.isVerifying -> {
            VerifyContent()
        }

        state.isVerified && state.retainsSession -> {
            VerifiedContent(
                description = stringResource(Res.string.email_verified_upgraded_desc),
                buttonText = stringResource(Res.string.email_verified_continue),
                onButtonClick = onExitClick,
            )
        }

        state.isVerified -> {
            VerifiedContent(
                description = stringResource(Res.string.email_verified_successfully_desc),
                buttonText = stringResource(Res.string.login),
                onButtonClick = onLoginClick,
            )
        }

        else -> {
            VerificationFailed(onCloseClick = onExitClick)
        }
    }
}

@Composable
private fun VerifyContent() {
    EmailVerificationLayout {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.verifying_account),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun VerifiedContent(
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
) {
    EmailVerificationLayout {
        Text(
            text = stringResource(Res.string.email_verified_successfully),
            color = MaterialTheme.colorScheme.extended.positive,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
        )
        TabMatesButton(
            modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth(),
            text = buttonText,
            onClick = onButtonClick,
        )
    }
}

@Composable
private fun VerificationFailed(onCloseClick: () -> Unit) {
    EmailVerificationLayout {
        Text(
            text = stringResource(Res.string.email_verified_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(Res.string.email_verified_failed_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        TabMatesButton(
            modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth(),
            text = stringResource(Res.string.close),
            style = TabMatesButtonStyle.Secondary,
            onClick = onCloseClick,
        )
    }
}

@Composable
private fun EmailVerificationLayout(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.heightIn(min = 200.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@PreviewThemes
@Composable
private fun EmailVerificationScreenVerifyingPreview() {
    TabMatesTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(isVerifying = true),
                onLoginClick = {},
                onExitClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EmailVerificationScreenVerifiedPreview() {
    TabMatesTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(isVerified = true),
                onLoginClick = {},
                onExitClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EmailVerificationScreenUpgradedPreview() {
    TabMatesTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(isVerified = true, retainsSession = true),
                onLoginClick = {},
                onExitClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EmailVerificationScreenErrorPreview() {
    TabMatesTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(),
                onLoginClick = {},
                onExitClick = {},
            )
        }
    }
}
