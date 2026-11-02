package de.tabmates.androidapp

import android.app.Application

class TabMatesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Play flavor registers the FCM notification channels; the FOSS flavor has no push and
        // supplies a no-op. See PlatformNotifications.kt in src/play and src/foss.
        installNotificationChannels()
    }
}
