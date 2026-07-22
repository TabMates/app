package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAvailability
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.biometric.BiometricResult
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.core.presentation.util.UiText
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_security_error
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ProfileViewModel(
    private val sessionStorage: SessionStorage,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val authService: AuthService,
    private val notificationPermissionController: NotificationPermissionController,
    private val pushNotificationController: PushNotificationController,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    private val selectedSection = MutableStateFlow(SettingsSection.PROFILE)

    // Hardware/enrollment state; refreshed on resume since the user may enroll in system settings.
    private val biometricAvailability = MutableStateFlow(biometricAuthenticator.availability())

    // Local preferences are grouped so the outer combine stays within its 5-argument limit.
    private val preferences =
        combine(
            appPreferencesRepository.themeMode(),
            appPreferencesRepository.notificationsEnabled(),
            appPreferencesRepository.biometricUnlockEnabled(),
        ) { themeMode, notificationsEnabled, biometricUnlockEnabled ->
            Preferences(themeMode, notificationsEnabled, biometricUnlockEnabled)
        }

    val state: StateFlow<ProfileState> =
        combine(
            sessionStorage.authState,
            preferences,
            selectedSection,
            notificationPermissionController.status,
            biometricAvailability,
        ) { auth, prefs, section, permissionStatus, biometric ->
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
                themeMode = prefs.themeMode,
                notificationsEnabled = prefs.notificationsEnabled,
                notificationsPermissionBlocked = permissionStatus == NotificationPermissionStatus.DENIED,
                biometricSupported = biometric != BiometricAvailability.UNSUPPORTED &&
                    biometric != BiometricAvailability.NO_HARDWARE,
                biometricAvailable = biometric == BiometricAvailability.AVAILABLE,
                biometricUnlockEnabled = prefs.biometricUnlockEnabled,
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

    /** Re-read biometric hardware/enrollment state (call when the screen resumes). */
    fun refreshBiometricAvailability() {
        biometricAvailability.value = biometricAuthenticator.availability()
    }

    /**
     * Toggle the biometric app-lock. Enabling first requires a successful authentication, both to
     * confirm the hardware works and to avoid locking the user out with an unusable credential.
     * [strings] are resolved by the UI so the OS prompt is localized.
     */
    fun onBiometricUnlockToggle(
        enabled: Boolean,
        strings: BiometricPromptStrings,
    ) {
        viewModelScope.launch {
            if (!enabled) {
                appPreferencesRepository.setBiometricUnlockEnabled(false)
                return@launch
            }
            when (biometricAuthenticator.authenticate(strings)) {
                BiometricResult.Success -> appPreferencesRepository.setBiometricUnlockEnabled(true)
                BiometricResult.Cancelled -> Unit // Leave the toggle off; no error to surface.
                is BiometricResult.Error ->
                    eventChannel.send(ProfileEvent.Error(UiText.Resource(Res.string.profile_security_error)))
            }
        }
    }

    private data class Preferences(
        val themeMode: ThemeMode,
        val notificationsEnabled: Boolean,
        val biometricUnlockEnabled: Boolean,
    )

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
