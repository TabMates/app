package de.tabmates.core.data

/** Public re-export of build-time app metadata. [BuildKonfig] itself is internal to this module. */
object AppBuildInfo {
    /** The installed app version (e.g. "1.2.0"), used by the app-update check. */
    val version: String = BuildKonfig.APP_VERSION

    /** This build's platform: `android`, `web`, `desktop` or `ios`. */
    val platform: String = clientPlatform

    /** `<platform>/<version>`, the exact form the backend's `X-Client-Version` header expects. */
    val clientVersionHeader: String = "$platform/$version"

    /**
     * Per-release build token proving this artifact really is the version it claims to be.
     *
     * `base64url(HMAC-SHA256(secret, "<platform>|<version>"))`, computed by CI at build time from a
     * secret this app never sees — only the finished token is baked in. Null on web (a token in a
     * browser bundle is readable in DevTools, so the server identifies web by its `Origin` instead)
     * and null in any build whose pipeline did not set `CLIENT_BUILD_TOKEN`.
     */
    val buildToken: String? = BuildKonfig.CLIENT_BUILD_TOKEN
}
