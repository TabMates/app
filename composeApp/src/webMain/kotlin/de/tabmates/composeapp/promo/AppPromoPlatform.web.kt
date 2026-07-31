package de.tabmates.composeapp.promo

import kotlinx.browser.window

private const val STANDALONE_QUERY = "(display-mode: standalone)"

actual fun isAndroidBrowser(): Boolean =
    window.navigator.userAgent.contains("Android", ignoreCase = true) &&
        !window.matchMedia(STANDALONE_QUERY).matches

actual fun openAppPromoTarget(url: String) {
    window.location.href = url
}
