package de.tabmates.composeapp.sync

import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.LocaleProvider
import de.tabmates.features.notifications.domain.PushNotificationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single

/**
 * Drives push registration from auth + the notifications toggle + language:
 * registers when logged in **and** notifications are enabled, unregisters on logout or when
 * the toggle is turned off, and re-registers when the in-app language changes while active.
 * A single [combine] keeps the ordering deterministic. Mirrors [GroupSyncCoordinator].
 */
@Single
class NotificationsSyncCoordinator(
    sessionStorage: SessionStorage,
    appPreferencesRepository: AppPreferencesRepository,
    localeProvider: LocaleProvider,
    private val pushNotificationController: PushNotificationController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        val loggedIn = sessionStorage.authState.map { it != null }.distinctUntilChanged()
        val enabled = appPreferencesRepository.notificationsEnabled().distinctUntilChanged()
        val languageTag = localeProvider.languageTag().distinctUntilChanged()

        var started = false
        combine(loggedIn, enabled, languageTag) { isLoggedIn, isEnabled, _ -> isLoggedIn && isEnabled }
            .onEach { active ->
                when {
                    active && !started -> {
                        started = true
                        pushNotificationController.start()
                    }
                    // Still active + already started -> only the language emitted -> re-register.
                    active -> pushNotificationController.refreshRegistration()
                    started -> {
                        started = false
                        pushNotificationController.stop()
                    }
                }
            }.launchIn(scope)
    }
}
