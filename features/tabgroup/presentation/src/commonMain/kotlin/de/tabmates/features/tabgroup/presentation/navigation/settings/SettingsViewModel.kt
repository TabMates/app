package de.tabmates.features.tabgroup.presentation.navigation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.auth.initials
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.authentication.domain.AuthService
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SettingsViewModel(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val authService: AuthService,
    private val notificationPermissionController: NotificationPermissionController,
    sessionStorage: SessionStorage,
) : ViewModel() {
    private val pendingMigrationEmail = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsState> =
        combine(
            sessionStorage.authState,
            appPreferencesRepository.themeMode(),
            appPreferencesRepository.notificationsEnabled(),
            notificationPermissionController.status,
            pendingMigrationEmail,
        ) { auth, themeMode, notificationsEnabled, permissionStatus, migrationEmail ->
            val user = auth?.user
            SettingsState(
                isLoading = false,
                username = user?.username.orEmpty(),
                email = user?.email.orEmpty(),
                initials = user?.initials.orEmpty(),
                isRegistered = user?.userType == UserType.REGISTERED,
                pendingMigrationEmail = migrationEmail,
                themeMode = themeMode,
                notificationsEnabled = notificationsEnabled,
                notificationsPermissionBlocked = permissionStatus == NotificationPermissionStatus.DENIED,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SettingsState(),
        )

    init {
        refreshNotificationPermission()
        refreshAccount()
    }

    /** Re-read OS permission (call when the screen resumes — user may change it in settings). */
    fun refreshNotificationPermission() {
        viewModelScope.launch { notificationPermissionController.refresh() }
    }

    /**
     * Re-reads the account (call when the screen resumes).
     *
     * An anonymous account becomes registered by confirming an emailed link, which usually happens
     * on whatever device the mailbox is on — so this device only learns about it by asking. The
     * failure case is deliberately silent: this decorates the screen, it does not gate it.
     */
    fun refreshAccount() {
        viewModelScope.launch {
            authService.refreshAccount().onSuccess { account ->
                // `pendingEmail` means an unconfirmed *change* of address for a registered user and
                // an unconfirmed *migration* for an anonymous one, so only the latter belongs here.
                pendingMigrationEmail.update {
                    account.pendingEmail.takeIf { account.user.userType == UserType.ANONYMOUS }
                }
            }
        }
    }

    fun onOpenNotificationSettings() {
        notificationPermissionController.openSettings()
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { appPreferencesRepository.setThemeMode(mode) }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch { appPreferencesRepository.setNotificationsEnabled(enabled) }
    }
}
