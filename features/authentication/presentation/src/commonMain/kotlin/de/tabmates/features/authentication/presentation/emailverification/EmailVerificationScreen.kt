package de.tabmates.features.authentication.presentation.emailverification

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.result.ResultBadge
import de.tabmates.core.designsystem.result.ResultLayout
import de.tabmates.core.designsystem.result.ResultTone
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.features.authentication.presentation.navigation.Login
import de.tabmates.features.authentication.presentation.navigation.Welcome
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.close
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_changed_desc
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_changed_title
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_continue
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_failed
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_failed_desc
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_failed_hint_guest
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_failed_hint_no_session
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_failed_hint_registered
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_successfully_desc
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_title
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_upgraded_desc
import tabmatesapp.features.authentication.presentation.generated.resources.email_verified_upgraded_title
import tabmatesapp.features.authentication.presentation.generated.resources.ic_check
import tabmatesapp.features.authentication.presentation.generated.resources.ic_link_off
import tabmatesapp.features.authentication.presentation.generated.resources.login
import tabmatesapp.features.authentication.presentation.generated.resources.verifying_account

/**
 * @param onExitClick Where leaving the screen leads: back into the app when the link was opened on a
 * live session — upgrading a guest keeps them signed in, and clearing the stack would strand a
 * perfectly valid session — and Welcome when there is no session to return to.
 * @param onContinueClick Where a guest lands after their upgrade is confirmed. Defaults to
 * [onExitClick] for the logged-out graph, which can never reach that outcome: with no account on the
 * device the token can only be finishing a registration.
 */
@Composable
fun EmailVerificationRoot(
    token: String,
    backStack: NavBackStack<NavKey>,
    onExitClick: () -> Unit,
    onContinueClick: () -> Unit = onExitClick,
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
        onContinueClick = onContinueClick,
        onExitClick = onExitClick,
    )
}

@Composable
private fun EmailVerificationScreen(
    state: EmailVerificationState,
    onLoginClick: () -> Unit,
    onContinueClick: () -> Unit,
    onExitClick: () -> Unit,
) {
    when (state.status) {
        VerificationStatus.Verifying -> {
            VerifyingContent()
        }

        VerificationStatus.Succeeded -> {
            when (state.origin) {
                VerificationOrigin.NoSession -> {
                    VerifiedContent(
                        title = stringResource(Res.string.email_verified_title),
                        description = stringResource(Res.string.email_verified_successfully_desc),
                        buttonText = stringResource(Res.string.login),
                        onButtonClick = onLoginClick,
                    )
                }

                VerificationOrigin.Guest -> {
                    VerifiedContent(
                        title = stringResource(Res.string.email_verified_upgraded_title),
                        description = stringResource(Res.string.email_verified_upgraded_desc),
                        buttonText = stringResource(Res.string.email_verified_continue),
                        onButtonClick = onContinueClick,
                    )
                }

                VerificationOrigin.Registered -> {
                    VerifiedContent(
                        title = stringResource(Res.string.email_verified_changed_title),
                        description = stringResource(Res.string.email_verified_changed_desc),
                        buttonText = stringResource(Res.string.login),
                        onButtonClick = onLoginClick,
                    )
                }
            }
        }

        VerificationStatus.Failed -> {
            VerificationFailed(origin = state.origin, onCloseClick = onExitClick)
        }
    }
}

@Composable
private fun VerifyingContent() {
    ResultLayout(
        title = stringResource(Res.string.verifying_account),
        badge = {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

@Composable
private fun VerifiedContent(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
) {
    ResultLayout(
        title = title,
        description = description,
        badge = {
            ResultBadge(
                icon = vectorResource(Res.drawable.ic_check),
                tone = ResultTone.Positive,
            )
        },
        actions = {
            TabMatesButton(
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
                text = buttonText,
                onClick = onButtonClick,
            )
        },
    )
}

@Composable
private fun VerificationFailed(
    origin: VerificationOrigin,
    onCloseClick: () -> Unit,
) {
    // The screen only ever holds a token, so it cannot send a new link itself. What it can do is
    // name the place that can, and that place differs per flow.
    val hint =
        when (origin) {
            VerificationOrigin.NoSession -> Res.string.email_verified_failed_hint_no_session
            VerificationOrigin.Guest -> Res.string.email_verified_failed_hint_guest
            VerificationOrigin.Registered -> Res.string.email_verified_failed_hint_registered
        }
    ResultLayout(
        title = stringResource(Res.string.email_verified_failed),
        description = stringResource(Res.string.email_verified_failed_desc),
        supportingText = stringResource(hint),
        badge = {
            ResultBadge(
                icon = vectorResource(Res.drawable.ic_link_off),
                tone = ResultTone.Error,
            )
        },
        actions = {
            TabMatesButton(
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
                text = stringResource(Res.string.close),
                style = TabMatesButtonStyle.Secondary,
                onClick = onCloseClick,
            )
        },
    )
}

@PreviewThemes
@Composable
private fun EmailVerificationScreenVerifyingPreview() {
    TabMatesTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(status = VerificationStatus.Verifying),
                onLoginClick = {},
                onContinueClick = {},
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
                state =
                    EmailVerificationState(
                        origin = VerificationOrigin.NoSession,
                        status = VerificationStatus.Succeeded,
                    ),
                onLoginClick = {},
                onContinueClick = {},
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
                state =
                    EmailVerificationState(
                        origin = VerificationOrigin.Guest,
                        status = VerificationStatus.Succeeded,
                    ),
                onLoginClick = {},
                onContinueClick = {},
                onExitClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EmailVerificationScreenEmailChangedPreview() {
    TabMatesTheme {
        Surface {
            EmailVerificationScreen(
                state =
                    EmailVerificationState(
                        origin = VerificationOrigin.Registered,
                        status = VerificationStatus.Succeeded,
                    ),
                onLoginClick = {},
                onContinueClick = {},
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
                state =
                    EmailVerificationState(
                        origin = VerificationOrigin.Guest,
                        status = VerificationStatus.Failed,
                    ),
                onLoginClick = {},
                onContinueClick = {},
                onExitClick = {},
            )
        }
    }
}
