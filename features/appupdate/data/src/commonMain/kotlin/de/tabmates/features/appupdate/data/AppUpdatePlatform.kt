package de.tabmates.features.appupdate.data

/** Platform identifier sent to the version endpoint so the backend returns the right store URL/version. */
expect val appUpdatePlatform: String

/** The web build is always current; used to skip the update check entirely. */
const val WEB_PLATFORM = "web"
