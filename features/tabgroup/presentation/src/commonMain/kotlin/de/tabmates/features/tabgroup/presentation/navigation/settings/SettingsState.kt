package de.tabmates.features.tabgroup.presentation.navigation.settings

import de.tabmates.core.domain.preferences.ThemeMode

data class SettingsState(
    val isLoading: Boolean = true,
    val username: String = "",
    val email: String = "",
    val initials: String = "",
    // Anonymous users have no email to show on the card, and something to gain from the upgrade.
    val isRegistered: Boolean = true,
    // Set while an anonymous account has asked to become a registered one and the emailed
    // confirmation link is still unredeemed.
    val pendingMigrationEmail: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    // OS notification permission is denied -> disable the toggle and show a banner.
    val notificationsPermissionBlocked: Boolean = false,
)
