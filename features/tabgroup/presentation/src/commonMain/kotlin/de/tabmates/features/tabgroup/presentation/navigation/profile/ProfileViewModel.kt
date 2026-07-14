package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.features.authentication.domain.AuthService
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import de.tabmates.features.notifications.domain.PushNotificationController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ProfileViewModel(
    private val sessionStorage: SessionStorage,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val authService: AuthService,
    private val notificationPermissionController: NotificationPermissionController,
    private val pushNotificationController: PushNotificationController,
) : ViewModel() {
    private val selectedSection = MutableStateFlow(SettingsSection.PROFILE)

    val state: StateFlow<ProfileState> =
        combine(
            sessionStorage.authState,
            appPreferencesRepository.themeMode(),
            appPreferencesRepository.notificationsEnabled(),
            selectedSection,
            notificationPermissionController.status,
        ) { auth, themeMode, notificationsEnabled, section, permissionStatus ->
            val user = auth?.user
            ProfileState(
                isLoading = false,
                username = user?.username.orEmpty(),
                email = user?.email.orEmpty(),
                initials =
                    user
                        ?.username
                        ?.take(2)
                        ?.uppercase()
                        .orEmpty(),
                isRegistered = user?.userType == UserType.REGISTERED,
                themeMode = themeMode,
                notificationsEnabled = notificationsEnabled,
                notificationsPermissionBlocked = permissionStatus == NotificationPermissionStatus.DENIED,
                selectedSection = section,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ProfileState(),
        )

    init {
        refreshNotificationPermission()
    }

    /** Re-read OS permission (call when the screen resumes — user may change it in settings). */
    fun refreshNotificationPermission() {
        viewModelScope.launch { notificationPermissionController.refresh() }
    }

    fun onOpenNotificationSettings() {
        notificationPermissionController.openSettings()
    }

    private val eventChannel = Channel<ProfileEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onSectionSelected(section: SettingsSection) {
        selectedSection.update { section }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { appPreferencesRepository.setThemeMode(mode) }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun onSignOut() {
        viewModelScope.launch {
            // Unregister the device token while the session is still valid — the DELETE needs the
            // bearer token, and both calls below clear it. Doing this after would 401 and silently
            // leave a stale token on the server, so the device would keep receiving pushes.
            pushNotificationController.stop()
            // Always clear the local session so sign-out works even if the network call fails.
            authService.logout(sessionStorage.get()?.refreshToken.orEmpty())
            sessionStorage.set(null)
            eventChannel.send(ProfileEvent.SignedOut)
        }
    }
}
