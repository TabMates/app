package de.tabmates.composeapp.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.app_lock_error
import tabmatesapp.composeapp.generated.resources.app_lock_prompt_cancel
import tabmatesapp.composeapp.generated.resources.app_lock_prompt_subtitle
import tabmatesapp.composeapp.generated.resources.app_lock_prompt_title
import tabmatesapp.composeapp.generated.resources.app_lock_sign_out
import tabmatesapp.composeapp.generated.resources.app_lock_subtitle
import tabmatesapp.composeapp.generated.resources.app_lock_title
import tabmatesapp.composeapp.generated.resources.app_lock_unlock
import tabmatesapp.composeapp.generated.resources.ic_lock

/**
 * Gates [content] behind the biometric app-lock.
 *
 * When the lock is engaged, [content] is not composed at all (so protected UI never renders behind
 * the lock); the OS prompt is triggered automatically on entering the locked state, with a manual
 * retry and a sign-out escape hatch. Also drives the background/foreground grace-period timing.
 *
 * The gate itself calls [AppLockViewModel.onSignedIn] indirectly via the auth callbacks; callers
 * should invoke it on fresh sign-in through [rememberAppLockViewModel] so a just-authenticated
 * session is not immediately re-locked.
 */
@Composable
fun BiometricLockGate(
    viewModel: AppLockViewModel,
    content: @Composable () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authInFlight by viewModel.authInFlight.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onEnteredBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEnteredForeground() }

    val promptStrings =
        BiometricPromptStrings(
            title = stringResource(Res.string.app_lock_prompt_title),
            subtitle = stringResource(Res.string.app_lock_prompt_subtitle),
            cancel = stringResource(Res.string.app_lock_prompt_cancel),
        )

    // Auto-present the OS prompt when the lock engages. Keyed on the state so it fires once per
    // transition into LOCKED (a cancelled attempt leaves the state unchanged, so it won't loop).
    // Returning from the background re-locks via onEnteredForeground(), which re-triggers this.
    LaunchedEffect(uiState) {
        if (uiState == AppLockUiState.LOCKED) viewModel.authenticate(promptStrings)
    }

    when (uiState) {
        AppLockUiState.UNLOCKED -> content()

        // Neutral splash while the enabled-flag is read, so protected content never flashes.
        AppLockUiState.RESOLVING ->
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}

        AppLockUiState.LOCKED ->
            LockScreen(
                showError = authError,
                authInFlight = authInFlight,
                onUnlock = { viewModel.authenticate(promptStrings) },
                onSignOut = viewModel::signOut,
            )
    }
}

/** Obtains the shared [AppLockViewModel] so callers can also notify it of fresh sign-ins. */
@Composable
fun rememberAppLockViewModel(): AppLockViewModel = koinViewModel()

@Composable
private fun LockScreen(
    showError: Boolean,
    authInFlight: Boolean,
    onUnlock: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 360.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_lock),
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp).size(32.dp),
                    )
                }
                VerticalSpacer(24.dp)
                Text(
                    text = stringResource(Res.string.app_lock_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                VerticalSpacer(8.dp)
                Text(
                    text = stringResource(Res.string.app_lock_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (showError) {
                    VerticalSpacer(8.dp)
                    Text(
                        text = stringResource(Res.string.app_lock_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                VerticalSpacer(32.dp)
                Button(
                    onClick = onUnlock,
                    enabled = !authInFlight,
                ) {
                    if (authInFlight) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(Res.string.app_lock_unlock))
                    }
                }
                VerticalSpacer(8.dp)
                TextButton(onClick = onSignOut) {
                    Text(
                        text = stringResource(Res.string.app_lock_sign_out),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
