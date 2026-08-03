package de.tabmates.core.presentation.format

/**
 * Spaces locales use as grouping separators. [Char.isWhitespace] does not cover these, and they are
 * written as code points so they cannot be mistaken for an ordinary space in the source.
 */
private val NON_BREAKING_SPACES =
    setOf(
        Char(0x00A0), // no-break space
        Char(0x202F), // narrow no-break space
        Char(0x2009), // thin space
    )

private const val FALLBACK_GROUPING_SIZE = 3

/**
 * Parses a user-typed or pasted amount. Accepts the locale's own notation, and stays tolerant of
 * the other convention — a hardware keyboard gives a `.` even when the locale wants a `,`.
 *
 * The trailing separator decides: it is grouping only when exactly [NumberSymbols.groupingSize]
 * digits follow it and it is not the locale's decimal separator. So `1.234` reads as 1234 in de-DE
 * but `1.5` reads as 1.5, and both `1,234.56` and `1.234,56` resolve to 1234.56 in their locale.
 */
fun parseAmount(
    input: String,
    symbols: NumberSymbols,
): Double? {
    val compact = input.filterNot { it.isWhitespace() || it in NON_BREAKING_SPACES }
    if (compact.isEmpty()) return null
    val negative = compact.startsWith('-') || compact.startsWith(symbols.minusSign)
    val body = compact.removePrefix("-").removePrefix(symbols.minusSign)
    if (body.isEmpty()) return null

    val separators = setOf('.', ',', symbols.decimalSeparator, symbols.groupingSeparator)
    val lastSeparator = body.indexOfLast { it in separators }
    val splitAt = if (lastSeparator >= 0 && !isGrouping(body, lastSeparator, symbols)) lastSeparator else -1
    val wholeText = if (splitAt < 0) body else body.substring(0, splitAt)
    val fractionText = if (splitAt < 0) "" else body.substring(splitAt + 1)

    if (wholeText.any { !it.isDigit() && it !in separators }) return null
    if (fractionText.any { !it.isDigit() }) return null
    val whole = wholeText.filter { it.isDigit() }
    if (whole.isEmpty() && fractionText.isEmpty()) return null

    val value = "${whole.ifEmpty { "0" }}.${fractionText.ifEmpty { "0" }}".toDoubleOrNull() ?: return null
    return if (negative) -value else value
}

private fun isGrouping(
    body: String,
    index: Int,
    symbols: NumberSymbols,
): Boolean {
    if (body[index] == symbols.decimalSeparator) return false
    val groupingSize = symbols.groupingSize.takeIf { it > 0 } ?: FALLBACK_GROUPING_SIZE
    return body.length - index - 1 == groupingSize
}
