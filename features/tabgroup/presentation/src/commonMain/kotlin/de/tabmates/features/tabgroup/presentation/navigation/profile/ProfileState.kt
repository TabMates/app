package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.domain.preferences.ThemeMode

/** Sections shown in the master list of the expanded (tablet/desktop/web) Settings layout. */
enum class SettingsSection {
    PROFILE,
    APPEARANCE,
    NOTIFICATIONS,
    SECURITY,
    ABOUT,
}

data class ProfileState(
    val isLoading: Boolean = true,
    val username: String = "",
    val email: String = "",
    val initials: String = "",
    // Anonymous users have no password/email to manage.
    val isRegistered: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    // OS notification permission is denied -> disable the toggle and show a banner.
    val notificationsPermissionBlocked: Boolean = false,
    // Device has biometric/credential hardware -> show the Security section.
    val biometricSupported: Boolean = false,
    // Biometrics (or a device credential) are enrolled -> the toggle can be turned on.
    val biometricAvailable: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val selectedSection: SettingsSection = SettingsSection.PROFILE,
)
