@file:OptIn(ExperimentalWasmJsInterop::class)

package de.tabmates.core.presentation.format

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

private const val SIGNED_PROBE_VALUE = -12345.6

actual fun platformNumberSymbols(): NumberSymbols =
    runCatching { wasmNumberProbes().toNumberSymbols() }.getOrDefault(NumberSymbols.Fallback)

private fun wasmNumberProbes(): NumberProbes {
    // Must be passed explicitly. Omitting it makes Intl use the *host* default locale, which is the
    // browser's UI language — not the content-language preference the rest of the app follows
    // (see DeviceLocale.web.kt). An English-UI browser set to German would then format as en-US.
    val locales = browserLocales()
    // Intl picks the currency symbol itself, so it is swapped for the marker after formatting.
    val currencySymbol = numberPart(locales, "currency", "currency", 1.0)
    val currencyProbe = formatNumber(locales, "currency", 1.0)
    return NumberProbes(
        decimalSeparator = numberPart(locales, "decimal", "decimal", SIGNED_PROBE_VALUE).firstOrNull() ?: '.',
        groupingSeparator = numberPart(locales, "decimal", "group", SIGNED_PROBE_VALUE).firstOrNull() ?: ',',
        minusSign = numberPart(locales, "decimal", "minusSign", SIGNED_PROBE_VALUE),
        percentSymbol = numberPart(locales, "percent", "percentSign", PERCENT_PROBE_VALUE),
        groupingProbe = formatNumber(locales, "decimal", GROUPING_PROBE_VALUE.toDouble()),
        currencyProbe = currencyProbe.replace(currencySymbol.ifEmpty { CURRENCY_MARKER }, CURRENCY_MARKER),
        percentProbe = formatNumber(locales, "percent", PERCENT_PROBE_VALUE),
    )
}

/** The full preference list, so Intl can fall through it the way the browser itself would. */
private fun browserLocales(): JsAny =
    js("navigator.languages&&navigator.languages.length?navigator.languages:[navigator.language||'en']")

/** Value of the first `t` part when `v` is formatted in style `s`, or "" when there is none. */
@Suppress("UNUSED_PARAMETER", "ktlint:standard:function-signature")
private fun numberPart(l: JsAny, s: String, t: String, v: Double): String =
    js("(Intl.NumberFormat(l,{style:s,currency:'USD'}).formatToParts(v).filter(p=>p.type==t)[0]||0).value||''")

@Suppress("UNUSED_PARAMETER", "ktlint:standard:function-signature")
private fun formatNumber(l: JsAny, s: String, v: Double): String =
    js("Intl.NumberFormat(l,{style:s,currency:'USD'}).format(v)")
