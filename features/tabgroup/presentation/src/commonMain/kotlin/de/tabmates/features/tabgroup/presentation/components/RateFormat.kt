package de.tabmates.features.tabgroup.presentation.components

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/**
 * Formats an exchange rate with [significantDigits] significant digits, trimming trailing zeros
 * (e.g. `0.92`, `149.3`, `0.00004`). Unlike [formatMoney], a fixed decimal count is wrong here:
 * low-value currencies would collapse to `0.0000`. Returns null for non-finite or non-positive
 * rates so callers can fold them into the missing-rate path.
 */
internal fun formatRate(
    rate: Double,
    significantDigits: Int = 4,
): String? {
    if (!rate.isFinite() || rate <= 0.0) return null
    val magnitude = floor(log10(rate)).toInt()
    val decimals = (significantDigits - 1 - magnitude).coerceIn(0, 8)
    val factor = 10.0.pow(decimals)
    val rounded = round(rate * factor) / factor
    val whole = rounded.toLong()
    val fraction = round((rounded - whole) * factor).toLong()
    if (decimals == 0 || fraction == 0L) return whole.toString()
    val fractionText = fraction.toString().padStart(decimals, '0').trimEnd('0')
    return "$whole.$fractionText"
}
