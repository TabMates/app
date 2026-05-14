package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

internal fun formatAmount(
    item: GroupOverviewItem,
    amount: Double,
): String = formatAmount(amount, item.currencySymbol, item.currencyDecimalDigits)

internal fun formatAmount(
    amount: Double,
    symbol: String,
    decimalDigits: Int,
): String {
    val digits = decimalDigits.coerceIn(0, 6)
    val factor = 10.0.pow(digits).toLong()
    val rounded = round(amount * factor).toLong()
    val whole = abs(rounded) / factor
    if (digits == 0) return "$symbol$whole"
    val frac = (abs(rounded) % factor).toString().padStart(digits, '0')
    return "$symbol$whole.$frac"
}
