package de.tabmates.androidapp

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Play-flavor notification setup: Firebase Cloud Messaging delivers pushes, so the app needs the
 * notification channels the backend routes by and the `POST_NOTIFICATIONS` runtime permission.
 *
 * The FOSS flavor supplies no-op twins of both functions — see the `foss` source set. Neither
 * belongs in `src/main`: `POST_NOTIFICATIONS` reaches the merged manifest only through the
 * Firebase AARs, so in a FOSS build requesting it would be denied instantly and strand the user in
 * the "open settings" prompt on every cold start.
 */
internal fun Application.installNotificationChannels() {
    NotificationChannels.register(this)
}

/** Which notification-permission prompt to surface, if any. */
private enum class NotificationPermissionPrompt {
    /** User denied but can be asked again — explain why, then re-request. */
    RATIONALE,

    /** Permanently denied ("don't ask again") — direct the user to app settings. */
    SETTINGS,
}

/**
 * Asks for `POST_NOTIFICATIONS` on first composition and surfaces the follow-up prompt when the
 * user says no. No-op below API 33, where the permission does not exist.
 */
@Composable
internal fun NotificationPermissionGate() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val activity = LocalActivity.current ?: return
    var prompt by remember { mutableStateOf<NotificationPermissionPrompt?>(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            prompt =
                when {
                    granted -> null

                    // Still askable -> show rationale and let the user retry.
                    activity.shouldExplainNotifications() -> NotificationPermissionPrompt.RATIONALE

                    // Permanently denied -> only the system settings screen can re-enable it.
                    else -> NotificationPermissionPrompt.SETTINGS
                }
        }

    LaunchedEffect(Unit) {
        if (context.hasNotificationPermission()) return@LaunchedEffect

        // If the system flags a prior denial, explain before re-asking; otherwise ask directly.
        if (activity.shouldExplainNotifications()) {
            prompt = NotificationPermissionPrompt.RATIONALE
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    prompt?.let { current ->
        // App() applies its own theme; this dialog sits outside it and needs one of its own.
        MaterialTheme {
            NotificationPermissionDialog(
                prompt = current,
                onConfirm = {
                    prompt = null
                    when (current) {
                        NotificationPermissionPrompt.RATIONALE -> {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        NotificationPermissionPrompt.SETTINGS -> {
                            context.openNotificationSettings()
                        }
                    }
                },
                onDismiss = { prompt = null },
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Context.hasNotificationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun android.app.Activity.shouldExplainNotifications(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        },
    )
}

@Composable
private fun NotificationPermissionDialog(
    prompt: NotificationPermissionPrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirmLabel =
        when (prompt) {
            NotificationPermissionPrompt.RATIONALE -> stringResource(R.string.notification_permission_allow)
            NotificationPermissionPrompt.SETTINGS -> stringResource(R.string.notification_permission_open_settings)
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.notification_permission_dismiss)) }
        },
    )
}
