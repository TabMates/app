package de.tabmates.features.tabgroup.presentation.navigation.profile

data class ProfileState(
    val isLoading: Boolean = true,
    val username: String = "",
    val email: String = "",
    val initials: String = "",
    // Anonymous users have no password/email to manage.
    val isRegistered: Boolean = true,
    // Set while an anonymous account has asked to become a registered one and the emailed
    // confirmation link is still unredeemed.
    val pendingMigrationEmail: String? = null,
    // Signing out wipes local data, so anything still queued is about to be lost.
    val pendingWriteCount: Int = 0,
    val showSignOutDialog: Boolean = false,
)
