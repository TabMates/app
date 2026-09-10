package de.tabmates.features.notifications.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidNotificationPermissionController(
    private val context: Context,
) : NotificationPermissionController {
    private val _status = MutableStateFlow(readStatus())
    override val status: StateFlow<NotificationPermissionStatus> = _status.asStateFlow()

    override suspend fun refresh() {
        _status.value = readStatus()
    }

    override fun openSettings() {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // areNotificationsEnabled() covers both the POST_NOTIFICATIONS runtime permission (API 33+)
    // and the user disabling notifications via the system settings.
    private fun readStatus(): NotificationPermissionStatus =
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationPermissionStatus.GRANTED
        } else {
            NotificationPermissionStatus.DENIED
        }
}
