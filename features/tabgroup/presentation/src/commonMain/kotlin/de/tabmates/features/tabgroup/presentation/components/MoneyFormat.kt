package de.tabmates.features.tabgroup.presentation.components

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * Canonical money formatter. Renders [amount] in [symbol] with [decimals] fraction digits,
 * prefixing a minus sign for negatives (e.g. `-€5.00`). Callers that drive their own +/- sign
 * (e.g. coloured balances) should pass a non-negative amount or use [formatMoneyUnsigned].
 */
internal fun formatMoney(
    symbol: String,
    amount: Double,
    decimals: Int,
): String {
    val digits = decimals.coerceIn(0, 6)
    val factor = 10.0.pow(digits)
    val rounded = round(amount * factor) / factor
    val negative = rounded < 0
    val abs = if (negative) -rounded else rounded
    val whole = abs.toLong()
    val fraction = round((abs - whole) * factor).toLong()
    val body =
        if (digits == 0) {
            whole.toString()
        } else {
            "$whole.${fraction.toString().padStart(digits, '0')}"
        }
    return buildString {
        if (negative) append('-')
        if (symbol.isNotEmpty()) append(symbol)
        append(body)
    }
}

/** Like [formatMoney] but always unsigned — for callers that render the +/- sign themselves. */
internal fun formatMoneyUnsigned(
    symbol: String,
    amount: Double,
    decimals: Int,
): String = formatMoney(symbol, abs(amount), decimals)
