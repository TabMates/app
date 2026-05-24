package de.tabmates.features.tabgroup.presentation.util

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun platformShortMonthNames(): List<String> {
    val formatter = NSDateFormatter().apply { locale = NSLocale.currentLocale }
    return formatter.shortMonthSymbols.take(12).map { (it as? String) ?: "—" }
}
