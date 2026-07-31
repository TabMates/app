package de.tabmates.composeapp.promo

// The user is already in the native app.
actual fun isAndroidBrowser(): Boolean = false

actual fun openAppPromoTarget(url: String) = Unit
