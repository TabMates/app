package de.tabmates.core.presentation.format

/** Stand-in for the currency symbol, so the affix probe can locate it unambiguously. */
internal const val CURRENCY_MARKER = "¤"

/** Grouping probe input — large enough to expose a second grouping level (e.g. Indian lakh). */
internal const val GROUPING_PROBE_VALUE = 10_000_000L

/** Percent probe input. */
internal const val PERCENT_PROBE_VALUE = 0.5

private const val DEFAULT_GROUPING_SIZE = 3

/**
 * Raw per-platform locale readings. Every actual fills this in and calls [toNumberSymbols], so the
 * affix and grouping derivation is written once instead of four times.
 *
 * Affixes are derived by probing rather than by reading a pattern: the platforms expose patterns in
 * mutually incompatible shapes, but they all agree on what a formatted number looks like.
 */
internal data class NumberProbes(
    val decimalSeparator: Char,
    val groupingSeparator: Char,
    val minusSign: String,
    val percentSymbol: String,
    /** [GROUPING_PROBE_VALUE] formatted with grouping enabled. */
    val groupingProbe: String,
    /** `1` formatted as currency, with the symbol replaced by [CURRENCY_MARKER]. */
    val currencyProbe: String,
    /** [PERCENT_PROBE_VALUE] formatted as a percentage. */
    val percentProbe: String,
)

internal fun NumberProbes.toNumberSymbols(): NumberSymbols {
    val grouping = groupingSizesOf(groupingProbe)
    val currency = affixOf(currencyProbe, CURRENCY_MARKER)
    val percent = affixOf(percentProbe, percentSymbol)
    return NumberSymbols(
        decimalSeparator = decimalSeparator,
        groupingSeparator = groupingSeparator,
        groupingSize = grouping.primary,
        secondaryGroupingSize = grouping.secondary,
        minusSign = minusSign.ifEmpty { NumberSymbols.Fallback.minusSign },
        percentSymbol = percentSymbol.ifEmpty { NumberSymbols.Fallback.percentSymbol },
        currencyBeforeAmount = currency.before,
        currencySpacing = currency.spacing,
        percentBeforeAmount = percent.before,
        percentSpacing = percent.spacing,
    )
}

internal data class Affix(
    val before: Boolean,
    val spacing: String,
)

/**
 * Locates [marker] relative to the digits in [probe]. Whatever sits between the two is the locale's
 * spacing, which is usually empty or a (narrow) no-break space.
 */
internal fun affixOf(
    probe: String,
    marker: String,
): Affix {
    val markerIndex = if (marker.isEmpty()) -1 else probe.indexOf(marker)
    val firstDigit = probe.indexOfFirst { it.isDigit() }
    val lastDigit = probe.indexOfLast { it.isDigit() }
    if (markerIndex < 0 || firstDigit < 0) return Affix(before = true, spacing = "")
    return if (markerIndex < firstDigit) {
        Affix(before = true, spacing = probe.substring(markerIndex + marker.length, firstDigit))
    } else {
        Affix(before = false, spacing = probe.substring(lastDigit + 1, markerIndex))
    }
}

internal data class GroupingSizes(
    val primary: Int,
    val secondary: Int,
)

/**
 * Reads the group lengths right-to-left out of a formatted [GROUPING_PROBE_VALUE]. The left-most
 * run of digits is the remainder rather than a full group, so it is dropped.
 */
internal fun groupingSizesOf(probe: String): GroupingSizes {
    val runs = mutableListOf<Int>()
    var run = 0
    for (char in probe) {
        if (char.isDigit()) {
            run++
        } else if (run > 0) {
            runs += run
            run = 0
        }
    }
    if (run > 0) runs += run
    // A single run means the locale does not group.
    if (runs.size <= 1) return GroupingSizes(primary = 0, secondary = 0)
    val groups = runs.drop(1)
    val primary = groups.last().takeIf { it > 0 } ?: DEFAULT_GROUPING_SIZE
    val secondary = if (groups.size > 1) groups[groups.size - 2] else primary
    return GroupingSizes(primary = primary, secondary = secondary)
}
