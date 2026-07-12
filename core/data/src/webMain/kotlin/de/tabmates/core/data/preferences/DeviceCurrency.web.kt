package de.tabmates.core.data.preferences

// No browser API exposes the OS default currency; the user picks one manually.
actual fun deviceCurrencyCode(): String? = null
