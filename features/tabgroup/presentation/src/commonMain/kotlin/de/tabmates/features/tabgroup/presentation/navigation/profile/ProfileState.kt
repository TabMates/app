package de.tabmates.features.tabgroup.presentation.navigation.profile

import de.tabmates.core.domain.preferences.ThemeMode

/** Sections shown in the master list of the expanded (tablet/desktop/web) Settings layout. */
enum class SettingsSection {
    PROFILE,
    APPEARANCE,
    NOTIFICATIONS,
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
    val selectedSection: SettingsSection = SettingsSection.PROFILE,
)
