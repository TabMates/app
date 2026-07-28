package de.tabmates.core.data

/**
 * Platform identifier this build reports to the backend.
 *
 * Two things depend on the exact spelling, so it lives here in `core/data` rather than in whichever
 * feature happened to need it first: the `X-Client-Version` header every request carries, and the
 * `platform` query parameter of `/api/app-version`. The server's `ClientPlatform` enum accepts
 * exactly these values (`android`, `web`, `desktop`, `ios`) and rejects the request outright on
 * anything else.
 */
expect val clientPlatform: String

/** The web build is always current — it cannot be out of date — and carries no build token. */
const val WEB_PLATFORM = "web"
