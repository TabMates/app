package de.tabmates.core.presentation.format

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterPercentStyle
import platform.Foundation.currentLocale
import platform.Foundation.numberWithDouble

actual fun platformNumberSymbols(): NumberSymbols =
    runCatching { iosNumberProbes().toNumberSymbols() }.getOrDefault(NumberSymbols.Fallback)

private fun iosNumberProbes(): NumberProbes {
    val current = NSLocale.currentLocale
    val decimal =
        NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterDecimalStyle
            locale = current
            usesGroupingSeparator = true
        }
    val currency =
        NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterCurrencyStyle
            locale = current
            currencySymbol = CURRENCY_MARKER
        }
    val percent =
        NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterPercentStyle
            locale = current
        }
    return NumberProbes(
        decimalSeparator = decimal.decimalSeparator.firstOrNull() ?: '.',
        groupingSeparator = decimal.groupingSeparator.firstOrNull() ?: ',',
        minusSign = decimal.minusSign.orEmpty(),
        percentSymbol = percent.percentSymbol.orEmpty(),
        groupingProbe = decimal.stringFromNumber(number(GROUPING_PROBE_VALUE.toDouble())).orEmpty(),
        currencyProbe = currency.stringFromNumber(number(1.0)).orEmpty(),
        percentProbe = percent.stringFromNumber(number(PERCENT_PROBE_VALUE)).orEmpty(),
    )
}

private fun number(value: Double): NSNumber = NSNumber.numberWithDouble(value)
