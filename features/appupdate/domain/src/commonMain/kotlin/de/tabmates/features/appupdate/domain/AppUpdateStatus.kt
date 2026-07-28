package de.tabmates.features.appupdate.domain

/** Outcome of an app-update check, derived from the locally installed version and the remote version info. */
sealed interface AppUpdateStatus {
    /** Installed version is current — nothing to do. */
    data object UpToDate : AppUpdateStatus

    /** A newer version exists but the current one still works — prompt is dismissible. */
    data class Optional(val updateUrl: String, val latestVersion: String) : AppUpdateStatus

    /**
     * Installed version is below the minimum supported one — the user must update to continue.
     *
     * [latestVersion] is null when the server refused this build (HTTP 426) without the version
     * check itself getting through: the update is certain, the number to update *to* is not.
     */
    data class Forced(val updateUrl: String, val latestVersion: String?) : AppUpdateStatus
}
