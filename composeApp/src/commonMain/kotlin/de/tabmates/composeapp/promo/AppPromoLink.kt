package de.tabmates.composeapp.promo

import de.tabmates.composeapp.BuildKonfig

internal const val ANDROID_PACKAGE = "de.tabmates.androidapp"

internal const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$ANDROID_PACKAGE"

/**
 * Path the promo aims at. It carries no state — its only job is to be a URL the Android app claims
 * in its manifest, so the intent below resolves. The app treats it as an unknown deep link and just
 * opens at home; on the web it 404s into `404.html`, which bounces to the root.
 *
 * Deliberately not `/`: claiming the bare host would make the installed app swallow every link to
 * the web client.
 */
private const val OPEN_PATH = "/open"

/**
 * Chrome's `intent://` syntax: resolve to [ANDROID_PACKAGE] if it is installed, otherwise follow
 * `browser_fallback_url` to the store listing. Both halves of the promo's promise in one URL.
 *
 * The host comes from `BASE_URL_PUBLIC` so it always matches the app's verified App Links host.
 */
internal fun androidAppPromoIntentUrl(): String {
    val host = BuildKonfig.BASE_URL_PUBLIC.trimEnd('/').substringAfter("://")
    return "intent://$host$OPEN_PATH#Intent;scheme=https;package=$ANDROID_PACKAGE;" +
        "S.browser_fallback_url=${percentEncode(PLAY_STORE_URL)};end"
}

private const val UNRESERVED = "-._~"

/**
 * Percent-encodes every character outside RFC 3986's unreserved set. The fallback URL is an intent
 * *extra*, so an unescaped `;` or `&` in it would terminate the extra and truncate the URL.
 */
private fun percentEncode(value: String): String =
    buildString(value.length) {
        value.encodeToByteArray().forEach { byte ->
            val char = byte.toInt().toChar()
            if ((char.isLetterOrDigit() && char.code < 128) || char in UNRESERVED) {
                append(char)
            } else {
                append('%')
                append(byte.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
