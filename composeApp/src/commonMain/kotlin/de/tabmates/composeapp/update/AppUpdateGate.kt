package de.tabmates.composeapp.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
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

/**
 * Runs the app-update check on first composition and surfaces the result.
 *
 * Android devices installed from the Play Store get the native in-app update flow; every other
 * platform (iOS, desktop, web, sideloaded Android) falls back to a dialog that opens the store URL.
 * See the platform [AppUpdateHandler] actuals.
 */
@Composable
fun AppUpdateGate() {
    val repository = koinInject<AppUpdateRepository>()
    var status by remember { mutableStateOf<AppUpdateStatus>(AppUpdateStatus.UpToDate) }

    LaunchedEffect(Unit) {
        status = repository.check()
    }

    AppUpdateHandler(status = status, onDismiss = { status = AppUpdateStatus.UpToDate })
}

/** Renders the update prompt for [status]. Android overrides this with the Play in-app update flow. */
@Composable
expect fun AppUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
)

/** Store-redirect fallback used by every non-Play platform (and by Android when Play is unavailable). */
@Composable
internal fun DefaultUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    when (status) {
        AppUpdateStatus.UpToDate -> Unit
        is AppUpdateStatus.Optional ->
            UpdateDialog(
                forced = false,
                onUpdate = { uriHandler.openUri(status.updateUrl) },
                onDismiss = onDismiss,
            )

        is AppUpdateStatus.Forced ->
            UpdateDialog(
                forced = true,
                onUpdate = { uriHandler.openUri(status.updateUrl) },
                onDismiss = onDismiss,
            )
    }
}

@Composable
private fun UpdateDialog(
    forced: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        // A forced update is not dismissible by tapping outside.
        onDismissRequest = { if (!forced) onDismiss() },
        title = { Text(stringResource(Res.string.update_dialog_title)) },
        text = {
            Text(
                stringResource(
                    if (forced) Res.string.update_dialog_message_forced else Res.string.update_dialog_message_optional,
                ),
            )
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
