package de.tabmates.core.presentation.format

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the per-platform readings themselves: the JS parsing on wasm and the formatter probes on
 * JVM/Apple all funnel through `runCatching`, so a broken one would silently degrade to
 * [NumberSymbols.Fallback] instead of failing. Asserts shape, never a specific locale's output —
 * the host locale is whatever the machine running the test is set to.
 */
class PlatformNumberSymbolsTest {
    @Test
    fun deviceSymbolsAreUsable() {
        val symbols = platformNumberSymbols()
        assertTrue(!symbols.decimalSeparator.isDigit(), "decimal separator must not be a digit")
        assertTrue(!symbols.groupingSeparator.isDigit(), "grouping separator must not be a digit")
        assertTrue(symbols.groupingSize >= 0)
        assertTrue(symbols.minusSign.isNotEmpty())
        assertTrue(symbols.percentSymbol.isNotEmpty())
    }

    @Test
    fun deviceSymbolsProduceReadableAmounts() {
        val symbols = platformNumberSymbols()
        val formatted = formatMoney("€", 1234.56, 2, symbols)
        assertTrue(formatted.contains("€"), formatted)
        assertTrue(formatted.count { it.isDigit() } == 6, formatted)
        assertTrue(formatted.contains(symbols.decimalSeparator), formatted)
        assertTrue(parseAmount(formatted.replace("€", ""), symbols) == 1234.56, formatted)
    }
}
