package de.tabmates.features.notifications.data.di

/**
 * Platform-specific Koin module providing the [de.tabmates.features.notifications.domain.PushNotificationController].
 * Mobile actuals back it with Firebase Cloud Messaging (kmpnotifier); desktop/web actuals provide a no-op.
 */
expect class PlatformNotificationsModule()
