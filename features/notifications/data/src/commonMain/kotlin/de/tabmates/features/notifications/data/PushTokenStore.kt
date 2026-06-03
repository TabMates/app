package de.tabmates.features.notifications.data

import de.tabmates.core.data.security.SecureStore
import org.koin.core.annotation.Single

/**
 * Caches the last device token registered with the backend, **encrypted** via [SecureStore]
 * (the shared vault), so the app can skip redundant re-registrations on launch.
 */
@Single
class PushTokenStore(
    secureStore: SecureStore,
) {
    private var token: String? by secureStore(null, key = "pushToken")

    fun lastRegistered(): String? = token

    fun setLastRegistered(value: String?) {
        token = value
    }
}
