package de.tabmates.core.domain.environment

import kotlinx.serialization.Serializable

/**
 * The backend the app talks to right now: the build-time default, or a custom one the user entered
 * on the welcome screen.
 *
 * Only the API surface is switchable. The user-facing public host (deep links, invite links) stays
 * on its build-time value — the Android manifest verifies exactly one App Links host, so rewriting
 * it at runtime would break incoming links without gaining anything.
 */
data class EnvironmentConfig(
    val httpBaseUrl: String,
    val wsBaseUrl: String,
    /** Null only on web, where the server allow-lists the browser Origin instead. */
    val apiKey: String?,
    val isCustom: Boolean,
)

/** What the user types in: everything else is derived from [httpBaseUrl]. */
@Serializable
data class CustomEnvironment(
    val httpBaseUrl: String,
    val apiKey: String,
)
