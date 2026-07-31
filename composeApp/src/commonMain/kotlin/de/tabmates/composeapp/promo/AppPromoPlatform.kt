package de.tabmates.composeapp.promo

/**
 * True only for the web build running in an Android browser tab.
 *
 * False in an installed PWA: opening the launcher icon is already a commitment to a home-screen
 * install, so nagging about the native app there is noise. Also false on every native target, where
 * the user is by definition already in the app.
 */
expect fun isAndroidBrowser(): Boolean

/**
 * Sends the browser to [url] as a top-level navigation.
 *
 * Deliberately not `LocalUriHandler.openUri`, which opens a new tab: the promo target is an
 * `intent://` URI, and handing that to a popup is both unreliable and blockable.
 *
 * No-op on every non-web target — nothing there ever calls it, since [isAndroidBrowser] gates it.
 */
expect fun openAppPromoTarget(url: String)
