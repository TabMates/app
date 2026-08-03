package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.core.domain.sync.PendingWrites
import de.tabmates.core.domain.util.onSuccess
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
    pendingWrites: PendingWrites,
) : ViewModel() {
    private val selectedSection = MutableStateFlow(SettingsSection.PROFILE)

    private val showSignOutDialog = MutableStateFlow(false)

    private val pendingMigrationEmail = MutableStateFlow<String?>(null)

    private val accountState =
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
        }

    // Layered onto the account state rather than folded in: `combine` tops out at five typed
    // sources and the block above already uses all five.
    val state: StateFlow<ProfileState> =
        combine(
            accountState,
            pendingWrites.observeCount(),
            showSignOutDialog,
            pendingMigrationEmail,
        ) { account, pendingWriteCount, showDialog, migrationEmail ->
            account.copy(
                pendingWriteCount = pendingWriteCount,
                showSignOutDialog = showDialog,
                pendingMigrationEmail = migrationEmail,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ProfileState(),
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

    /**
     * Signing out wipes this device's local data, including writes that never reached the server,
     * so anything still queued has to be confirmed away rather than silently dropped. An anonymous
     * account is always confirmed: it only exists on this device and has no credentials to sign
     * back in with, so signing out destroys it outright.
     */
    fun onSignOutClick() {
        if (state.value.pendingWriteCount > 0 || !state.value.isRegistered) {
            showSignOutDialog.update { true }
        } else {
            signOut()
        }
    }

    fun onDismissSignOutDialog() {
        showSignOutDialog.update { false }
    }

    fun onConfirmSignOut() {
        showSignOutDialog.update { false }
        signOut()
    }

    private fun signOut() {
        viewModelScope.launch {
            // Unregister the device token while the session is still valid — the DELETE needs the
            // bearer token, and both calls below clear it. Doing this after would 401 and silently
            // leave a stale token on the server, so the device would keep receiving pushes.
            pushNotificationController.stop()
            // Result intentionally ignored: a failed revoke leaves the refresh token live on the
            // server until it expires, which is the accepted cost of sign-out always working —
            // stranding the user in a signed-in app because the network was down is worse.
            authService.logout(sessionStorage.get()?.refreshToken.orEmpty())
            sessionStorage.set(null)
            eventChannel.send(ProfileEvent.SignedOut)
        }
    }
}
