package de.tabmates.features.tabgroup.presentation.util

import java.text.DateFormatSymbols
import java.util.Locale

actual fun platformShortMonthNames(): List<String> =
    DateFormatSymbols
        .getInstance(Locale.getDefault())
        .shortMonths
        .take(12)
        .map { it.ifEmpty { "—" } }
