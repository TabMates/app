package de.tabmates.androidapp

import android.app.Application

class TabMatesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.register(this)
    }
}
