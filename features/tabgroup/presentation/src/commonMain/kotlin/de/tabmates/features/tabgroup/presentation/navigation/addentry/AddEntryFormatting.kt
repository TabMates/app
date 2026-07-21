package de.tabmates.features.tabgroup.presentation.navigation.addentry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.tabmates.features.tabgroup.presentation.util.platformShortMonthNames
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

internal fun parseAmount(input: String): Double? {
    if (input.isBlank()) return null
    val normalized = input.replace(',', '.').trim()
    return normalized.toDoubleOrNull()
}

@Composable
internal fun rememberMonthAbbreviations(): List<String> = remember { platformShortMonthNames() }

internal fun formatEntryDate(
    date: LocalDate,
    monthAbbreviations: List<String>,
): String {
    val name = monthAbbreviations.getOrNull(date.month.number - 1) ?: return date.toString()
    return "$name ${date.day}"
}
