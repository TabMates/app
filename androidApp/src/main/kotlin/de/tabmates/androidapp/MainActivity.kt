package de.tabmates.androidapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import de.tabmates.composeapp.App
import de.tabmates.composeapp.deeplink.DeepLinkHandler

/** Which notification-permission prompt to surface, if any. */
private enum class NotificationPermissionPrompt {
    /** User denied but can be asked again — explain why, then re-request. */
    RATIONALE,

    /** Permanently denied ("don't ask again") — direct the user to app settings. */
    SETTINGS,
}

class MainActivity : ComponentActivity() {
    private var notificationPrompt by mutableStateOf<NotificationPermissionPrompt?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPrompt =
                    when {
                        granted -> null

                        // Still askable -> show rationale and let the user retry.
                        shouldExplainNotifications() -> NotificationPermissionPrompt.RATIONALE

                        // Permanently denied -> only the system settings screen can re-enable it.
                        else -> NotificationPermissionPrompt.SETTINGS
                    }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)
        requestNotificationPermissionIfNeeded()

        setContent {
            App()
            notificationPrompt?.let { prompt ->
                MaterialTheme {
                    NotificationPermissionDialog(
                        prompt = prompt,
                        onConfirm = {
                            notificationPrompt = null
                            when (prompt) {
                                NotificationPermissionPrompt.RATIONALE -> {
                                    if (Build.VERSION.SDK_INT >=
                                        Build.VERSION_CODES.TIRAMISU
                                    ) {
                                        launchNotificationRequest()
                                    }
                                }

                                NotificationPermissionPrompt.SETTINGS -> {
                                    openNotificationSettings()
                                }
                            }
                        },
                        onDismiss = { notificationPrompt = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data?.toString() ?: return
        DeepLinkHandler.onDeepLink(uri)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) return

        // If the system flags a prior denial, explain before re-asking; otherwise ask directly.
        if (shouldExplainNotifications()) {
            notificationPrompt = NotificationPermissionPrompt.RATIONALE
        } else {
            launchNotificationRequest()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun shouldExplainNotifications(): Boolean =
        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun launchNotificationRequest() {
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
        )
    }
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

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
