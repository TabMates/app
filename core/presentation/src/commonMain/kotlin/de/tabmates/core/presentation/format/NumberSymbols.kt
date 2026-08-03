package de.tabmates.core.presentation.format

import androidx.compose.runtime.Immutable

/**
 * Locale-derived formatting data — values only, no logic. The platform supplies these; the actual
 * grouping and assembly lives in [formatMoney] and friends so the output stays identical across
 * targets (ICU versions disagree on whole formatted strings, but not on these building blocks).
 */
@Immutable
data class NumberSymbols(
    val decimalSeparator: Char,
    val groupingSeparator: Char,
    /** Digits per group, counted from the right. Zero means the locale does not group at all. */
    val groupingSize: Int,
    /** Digits per group above the first one — locales like hi-IN group 3 then 2 ("1,00,00,000"). */
    val secondaryGroupingSize: Int,
    val minusSign: String,
    val percentSymbol: String,
    val currencyBeforeAmount: Boolean,
    val currencySpacing: String,
    val percentBeforeAmount: Boolean,
    val percentSpacing: String,
) {
    companion object {
        /** en-US shaped defaults, used when a platform lookup fails. */
        val Fallback =
            NumberSymbols(
                decimalSeparator = '.',
                groupingSeparator = ',',
                groupingSize = 3,
                secondaryGroupingSize = 3,
                minusSign = "-",
                percentSymbol = "%",
                currencyBeforeAmount = true,
                currencySpacing = "",
                percentBeforeAmount = false,
                percentSpacing = "",
            )
    }
}

/** Reads [NumberSymbols] from the device locale. */
expect fun platformNumberSymbols(): NumberSymbols
