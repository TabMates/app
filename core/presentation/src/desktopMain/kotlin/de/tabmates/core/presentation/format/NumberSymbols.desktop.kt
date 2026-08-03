package de.tabmates.core.presentation.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

// Duplicated in NumberSymbols.android.kt: androidMain and desktopMain share no parent source set,
// same as DeviceCurrency.android.kt / .desktop.kt.
actual fun platformNumberSymbols(): NumberSymbols =
    runCatching { jvmNumberProbes(Locale.getDefault()).toNumberSymbols() }
        .getOrDefault(NumberSymbols.Fallback)

private fun jvmNumberProbes(locale: Locale): NumberProbes {
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val grouping = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = true }
    val percent = NumberFormat.getPercentInstance(locale)
    val currency =
        (NumberFormat.getCurrencyInstance(locale) as DecimalFormat).apply {
            decimalFormatSymbols = decimalFormatSymbols.apply { currencySymbol = CURRENCY_MARKER }
        }
    return NumberProbes(
        decimalSeparator = symbols.decimalSeparator,
        groupingSeparator = symbols.groupingSeparator,
        minusSign = symbols.minusSign.toString(),
        percentSymbol = symbols.percent.toString(),
        groupingProbe = grouping.format(GROUPING_PROBE_VALUE),
        currencyProbe = currency.format(1),
        percentProbe = percent.format(PERCENT_PROBE_VALUE),
    )
}
