package de.tabmates.core.presentation.format

/** Written as code points: an NBSP is indistinguishable from a space in source. */
internal val NBSP = Char(0x00A0)
internal val NARROW_NBSP = Char(0x202F)

/**
 * Hand-built locale readings. Tests assert against these rather than against the device locale, so
 * they stay identical on every target regardless of the host's ICU version.
 */
internal val EN_US = NumberSymbols.Fallback

internal val DE_DE =
    NumberSymbols(
        decimalSeparator = ',',
        groupingSeparator = '.',
        groupingSize = 3,
        secondaryGroupingSize = 3,
        minusSign = "-",
        percentSymbol = "%",
        currencyBeforeAmount = false,
        currencySpacing = NBSP.toString(),
        percentBeforeAmount = false,
        percentSpacing = NBSP.toString(),
    )

internal val DE_CH =
    DE_DE.copy(
        decimalSeparator = '.',
        groupingSeparator = '\'',
        currencyBeforeAmount = true,
        percentSpacing = "",
    )

internal val FR_FR =
    DE_DE.copy(groupingSeparator = NARROW_NBSP)

/** Groups the first three digits and then every two: `1,00,00,000`. */
internal val HI_IN = EN_US.copy(secondaryGroupingSize = 2)

/** Turkish writes the percent sign in front of the number. */
internal val TR_TR = DE_DE.copy(percentBeforeAmount = true, percentSpacing = "")

internal val NO_GROUPING = EN_US.copy(groupingSize = 0, secondaryGroupingSize = 0)
