package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import de.tabmates.features.tabgroup.presentation.BuildKonfig

// Shared invite links point at the user-facing public host (e.g. https://app.tabmates.de/j/…),
// which resolves via App Links when the app is installed and via the web client otherwise.
private val invitePrefix: String
    get() = "${BuildKonfig.BASE_URL_PUBLIC.trimEnd('/')}/j/"

internal fun buildInviteUrl(inviteToken: String): String = "$invitePrefix$inviteToken"

internal fun shortInviteUrl(inviteToken: String): String {
    val noScheme = invitePrefix.substringAfter("://")
    return "$noScheme$inviteToken"
}
