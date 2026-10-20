package de.tabmates.composeapp.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import de.tabmates.composeapp.BuildKonfig
import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.domain.update.UpgradeRequiredNotifier
import de.tabmates.features.appupdate.data.AppUpdateRepository
import de.tabmates.features.appupdate.domain.AppUpdateStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.update_dialog_confirm
import tabmatesapp.composeapp.generated.resources.update_dialog_dismiss
import tabmatesapp.composeapp.generated.resources.update_dialog_message_forced
import tabmatesapp.composeapp.generated.resources.update_dialog_message_optional
import tabmatesapp.composeapp.generated.resources.update_dialog_title
import tabmatesapp.composeapp.generated.resources.update_dialog_version_current
import tabmatesapp.composeapp.generated.resources.update_dialog_version_latest

/**
 * Runs the app-update check on first composition and surfaces the result.
 *
 * Android devices installed from the Play Store get the native in-app update flow; every other
 * platform (iOS, desktop, web, sideloaded Android) falls back to a dialog that opens the store URL.
 * See the platform [AppUpdateHandler] actuals.
 *
 * The startup check is not the only trigger: the backend can start refusing this build at any point
 * in the session (HTTP 426), and [UpgradeRequiredNotifier] escalates that to the same forced prompt.
 */
@Composable
fun AppUpdateGate() {
    val repository = koinInject<AppUpdateRepository>()
    val upgradeRequiredNotifier = koinInject<UpgradeRequiredNotifier>()
    var status by remember { mutableStateOf<AppUpdateStatus>(AppUpdateStatus.UpToDate) }
    val upgradeRequired by upgradeRequiredNotifier.isUpgradeRequired.collectAsState()

    LaunchedEffect(Unit) {
        status = repository.check()
    }

    LaunchedEffect(upgradeRequired) {
        if (!upgradeRequired) return@LaunchedEffect
        // Ask again first: the server knows the real store URL, and by now it may also report a
        // newer minimum than the startup check saw. If that call fails — or the gate blocks it —
        // fall back to the public site, which always has a way to get the current build.
        status =
            repository.check().takeIf { it is AppUpdateStatus.Forced }
                ?: AppUpdateStatus.Forced(BuildKonfig.BASE_URL_PUBLIC, latestVersion = null)
    }

    AppUpdateHandler(status = status, onDismiss = { status = AppUpdateStatus.UpToDate })
}

/** Renders the update prompt for [status]. Android overrides this with the Play in-app update flow. */
@Composable
expect fun AppUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
)

/**
 * Store-redirect fallback used by every non-Play platform (and by Android when Play is unavailable).
 *
 * [updateUrlOverride] replaces the URL the backend supplied. The server answers per *platform*,
 * not per distribution, so `platform=android` always names the Play listing — which is the wrong
 * destination for a build that was not installed from Play. Only the FOSS Android handler sets it.
 */
@Composable
internal fun DefaultUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
    updateUrlOverride: String? = null,
) {
    val uriHandler = LocalUriHandler.current
    when (status) {
        AppUpdateStatus.UpToDate -> Unit
        is AppUpdateStatus.Optional ->
            UpdateDialog(
                forced = false,
                latestVersion = status.latestVersion,
                onUpdate = { uriHandler.openUri(updateUrlOverride ?: status.updateUrl) },
                onDismiss = onDismiss,
            )

        is AppUpdateStatus.Forced ->
            UpdateDialog(
                forced = true,
                latestVersion = status.latestVersion,
                onUpdate = { uriHandler.openUri(updateUrlOverride ?: status.updateUrl) },
                onDismiss = onDismiss,
            )
    }
}

@Composable
private fun UpdateDialog(
    forced: Boolean,
    latestVersion: String?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        // A forced update is not dismissible by tapping outside.
        onDismissRequest = { if (!forced) onDismiss() },
        title = { Text(stringResource(Res.string.update_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        if (forced) {
                            Res.string.update_dialog_message_forced
                        } else {
                            Res.string.update_dialog_message_optional
                        },
                    ),
                )
                // Names the versions involved: without them the user cannot tell an update they
                // already installed from one they still owe. Latest is unknown on the 426 path,
                // where the line is simply left out.
                Column {
                    Text(
                        text = stringResource(Res.string.update_dialog_version_current, AppBuildInfo.version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (latestVersion != null) {
                        Text(
                            text = stringResource(Res.string.update_dialog_version_latest, latestVersion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) { Text(stringResource(Res.string.update_dialog_confirm)) }
        },
        dismissButton =
            if (forced) {
                null
            } else {
                { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.update_dialog_dismiss)) } }
            },
    )
}
