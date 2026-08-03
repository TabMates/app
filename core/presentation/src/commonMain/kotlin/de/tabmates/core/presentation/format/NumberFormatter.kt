package de.tabmates.core.presentation.format

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/** Fraction digits to assume when a currency does not tell us how many it has. */
const val DEFAULT_CURRENCY_DECIMALS = 2

/** Significant digits used for exchange rates. */
const val DEFAULT_RATE_SIGNIFICANT_DIGITS = 4

private const val MAX_FRACTION_DIGITS = 8
private const val PERCENT_FRACTION_DIGITS = 2

/** Beyond 2^53 a Double no longer represents consecutive integers, so grouping would be a lie. */
private const val MAX_EXACT_SCALED = 9_007_199_254_740_992.0

/**
 * The sign shown in front of a balance. Balance screens pick it from the debt direction and pair it
 * with a colour, so it cannot be derived from the number — those amounts are always magnitudes.
 */
enum class AmountSign(
    internal val glyph: String,
) {
    None(""),
    Positive("+"),

    /** U+2212, which pairs visually with the plus. Ordinary negatives use the locale minus. */
    Negative("−"),
}

/**
 * Canonical money formatter. Renders [amount] with [decimals] fraction digits using the locale's
 * separators and grouping, placing [symbol] on the side the locale puts it (`€1,234.56` in en-US,
 * `1.234,56 €` in de-DE). Negatives get the locale minus sign in front.
 *
 * [symbol] and [decimals] come from the server's `Currency`, not from the locale — the same
 * currency has to look the same on every device.
 */
fun formatMoney(
    symbol: String,
    amount: Double,
    decimals: Int,
    symbols: NumberSymbols,
): String =
    buildString {
        if (isNegativeAfterRounding(amount, decimals)) append(symbols.minusSign)
        append(withCurrency(formatDigits(amount, decimals, symbols), symbol, symbols))
    }

/** Like [formatMoney] but always unsigned — for callers that render the sign themselves. */
fun formatMoneyUnsigned(
    symbol: String,
    amount: Double,
    decimals: Int,
    symbols: NumberSymbols,
): String = formatMoney(symbol, abs(amount), decimals, symbols)

/** Renders `|amount|` behind an explicit [sign], for coloured balances. See [AmountSign]. */
fun formatSignedMoney(
    symbol: String,
    amount: Double,
    decimals: Int,
    symbols: NumberSymbols,
    sign: AmountSign,
): String = sign.glyph + formatMoneyUnsigned(symbol, amount, decimals, symbols)

/**
 * Renders [value] as a percentage, including the locale's percent symbol and its spacing
 * (`50%` in en-US, `50 %` in de-DE). Trailing zeros are dropped, so a whole percentage reads `50`.
 * [value] is already in percent, not a 0..1 fraction.
 */
fun formatPercent(
    value: Double,
    symbols: NumberSymbols,
): String {
    val body = trimTrailingZeros(formatDigits(value, PERCENT_FRACTION_DIGITS, symbols), symbols)
    return buildString {
        if (isNegativeAfterRounding(value, PERCENT_FRACTION_DIGITS)) append(symbols.minusSign)
        if (symbols.percentBeforeAmount) {
            append(symbols.percentSymbol).append(symbols.percentSpacing).append(body)
        } else {
            append(body).append(symbols.percentSpacing).append(symbols.percentSymbol)
        }
    }
}

/**
 * Formats an exchange rate with [significantDigits] significant digits, trimming trailing zeros
 * (e.g. `0.92`, `149.3`, `0.00004`). Unlike [formatMoney], a fixed decimal count is wrong here:
 * low-value currencies would collapse to `0.0000`. Returns null for non-finite or non-positive
 * rates so callers can fold them into the missing-rate path.
 */
fun formatRate(
    rate: Double,
    symbols: NumberSymbols,
    significantDigits: Int = DEFAULT_RATE_SIGNIFICANT_DIGITS,
): String? {
    if (!rate.isFinite() || rate <= 0.0) return null
    val magnitude = floor(log10(rate)).toInt()
    val decimals = (significantDigits - 1 - magnitude).coerceIn(0, MAX_FRACTION_DIGITS)
    return trimTrailingZeros(formatDigits(rate, decimals, symbols), symbols)
}

/**
 * Renders [value] for an editable amount field: locale decimal separator, but deliberately no
 * grouping. Grouped text is awkward to edit and every keystroke would have to undo it again.
 */
fun formatAmountForInput(
    value: Double,
    decimals: Int,
    symbols: NumberSymbols,
): String =
    buildString {
        if (isNegativeAfterRounding(value, decimals)) append(symbols.minusSign)
        append(formatDigits(value, decimals, symbols.copy(groupingSize = 0)))
    }

/** Half of the smallest representable amount — the tolerance for comparing rounded money. */
fun amountEpsilon(decimals: Int): Double = 0.5 / 10.0.pow(decimals.coerceIn(0, MAX_FRACTION_DIGITS))

/** Rounds [amount] to [decimals] fraction digits, matching what [formatMoney] would display. */
fun roundToDecimals(
    amount: Double,
    decimals: Int,
): Double {
    val factor = 10.0.pow(decimals.coerceIn(0, MAX_FRACTION_DIGITS))
    val rounded = roundHalfUp(abs(amount) * factor) / factor
    return if (amount < 0.0) -rounded else rounded
}

/**
 * Rounds a non-negative value with ties going up, the convention money is expected to follow.
 * [kotlin.math.round] breaks ties towards the *even* integer, so `0.125` would render as `0.12`
 * while `0.375` renders as `0.38`. Only exact ties differ, so everything else defers to [round]
 * rather than the `floor(x + 0.5)` trick, which misrounds values just below a tie.
 *
 * This cannot make decimal rounding exact: `1.005 * 100` is already `100.49999999999999` as a
 * Double, so it rounds down under either convention. Fixing that needs decimal arithmetic.
 */
private fun roundHalfUp(value: Double): Double {
    val whole = floor(value)
    return if (value - whole == 0.5) whole + 1.0 else round(value)
}

/** Unsigned digits of [value], grouped per [symbols]. The sign is the caller's business. */
private fun formatDigits(
    value: Double,
    decimals: Int,
    symbols: NumberSymbols,
): String {
    val digits = decimals.coerceIn(0, MAX_FRACTION_DIGITS)
    val magnitude = abs(value)
    val scaled = roundHalfUp(magnitude * 10.0.pow(digits))
    if (!scaled.isFinite() || scaled >= MAX_EXACT_SCALED) {
        // Past Double's integer precision, so grouping the digits would be meaningless anyway.
        return magnitude.toString().replace('.', symbols.decimalSeparator)
    }
    val text = scaled.toLong().toString().padStart(digits + 1, '0')
    val whole = text.substring(0, text.length - digits)
    val fraction = text.substring(text.length - digits)
    return buildString {
        append(groupDigits(whole, symbols))
        if (digits > 0) append(symbols.decimalSeparator).append(fraction)
    }
}

private fun groupDigits(
    whole: String,
    symbols: NumberSymbols,
): String {
    val primary = symbols.groupingSize
    if (primary <= 0 || whole.length <= primary) return whole
    val secondary = symbols.secondaryGroupingSize.takeIf { it > 0 } ?: primary
    val groups = ArrayDeque<String>()
    var end = whole.length
    var size = primary
    while (end > 0) {
        val start = (end - size).coerceAtLeast(0)
        groups.addFirst(whole.substring(start, end))
        end = start
        size = secondary
    }
    return groups.joinToString(symbols.groupingSeparator.toString())
}

/**
 * Whether [amount] still reads as negative once rounded to [decimals] — `-0.001` must not render
 * as `-0.00`.
 */
private fun isNegativeAfterRounding(
    amount: Double,
    decimals: Int,
): Boolean {
    if (amount >= 0.0 || !amount.isFinite()) return false
    // Must match formatDigits, or "-0.005" could show a minus in front of "0.00".
    return roundHalfUp(abs(amount) * 10.0.pow(decimals.coerceIn(0, MAX_FRACTION_DIGITS))) > 0.0
}

private fun withCurrency(
    body: String,
    symbol: String,
    symbols: NumberSymbols,
): String =
    when {
        symbol.isEmpty() -> body
        symbols.currencyBeforeAmount -> symbol + symbols.currencySpacing + body
        else -> body + symbols.currencySpacing + symbol
    }

/** Drops a `0`-only tail. Safe on grouped text: trimming stops at the decimal separator. */
private fun trimTrailingZeros(
    text: String,
    symbols: NumberSymbols,
): String {
    if (!text.contains(symbols.decimalSeparator)) return text
    return text.trimEnd('0').trimEnd(symbols.decimalSeparator)
}
