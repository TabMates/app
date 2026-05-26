package de.tabmates.features.tabgroup.presentation.navigation.addexpense

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.tabmates.features.tabgroup.presentation.util.platformShortMonthNames
import kotlin.math.pow
import kotlin.math.round
import kotlin.time.Instant

internal fun formatMoney(
    symbol: String,
    amount: Double,
    decimals: Int,
): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(amount * factor) / factor
    val negative = rounded < 0
    val abs = if (negative) -rounded else rounded
    val whole = abs.toLong()
    val fraction = (round((abs - whole) * factor)).toLong()
    val fractionStr = fraction.toString().padStart(decimals, '0')
    val body =
        if (decimals == 0) {
            whole.toString()
        } else {
            "$whole.$fractionStr"
        }
    return buildString {
        if (negative) append('-')
        if (symbol.isNotEmpty()) append(symbol)
        append(body)
    }
}

internal fun parseAmount(input: String): Double? {
    if (input.isBlank()) return null
    val normalized = input.replace(',', '.').trim()
    return normalized.toDoubleOrNull()
}

@Composable
internal fun rememberMonthAbbreviations(): List<String> = remember { platformShortMonthNames() }

internal fun formatExpenseDate(
    instant: Instant,
    monthAbbreviations: List<String>,
): String {
    val iso = instant.toString()
    val datePart = iso.substringBefore('T')
    val pieces = datePart.split('-')
    if (pieces.size < 3) return datePart
    val month = pieces[1].toIntOrNull() ?: return datePart
    val day = pieces[2].toIntOrNull() ?: return datePart
    val name = monthAbbreviations.getOrNull(month - 1) ?: return datePart
    return "$name $day"
}
