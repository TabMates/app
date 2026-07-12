package de.tabmates.core.data.preferences

import platform.Foundation.NSLocale
import platform.Foundation.currencyCode
import platform.Foundation.currentLocale

actual fun deviceCurrencyCode(): String? = NSLocale.currentLocale.currencyCode
