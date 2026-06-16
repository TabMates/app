package de.tabmates.features.appupdate.data

import kotlinx.serialization.Serializable

/** Remote version info returned by `GET /api/app-version` for a given platform. */
@Serializable
data class AppVersionDto(
    /** Latest version available in the store for this platform. */
    val latestVersion: String,
    /** Oldest version still allowed to run; below this an update is forced. */
    val minSupportedVersion: String,
    /** Store / download page to send the user to (Play, App Store, or desktop download). */
    val updateUrl: String,
)
