package de.tabmates.core.data

/** Public re-export of build-time app metadata. [BuildKonfig] itself is internal to this module. */
object AppBuildInfo {
    /** The installed app version (e.g. "1.2.0"), used by the app-update check. */
    val version: String = BuildKonfig.APP_VERSION
}
