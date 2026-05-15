package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import de.tabmates.features.tabgroup.presentation.BuildKonfig

private val invitePrefix: String
    get() = "${BuildKonfig.BASE_URL_HTTP.trimEnd('/')}/j/"

internal fun buildInviteUrl(inviteToken: String): String = "$invitePrefix$inviteToken"

internal fun shortInviteUrl(inviteToken: String): String {
    val noScheme = invitePrefix.substringAfter("://")
    return "$noScheme$inviteToken"
}
