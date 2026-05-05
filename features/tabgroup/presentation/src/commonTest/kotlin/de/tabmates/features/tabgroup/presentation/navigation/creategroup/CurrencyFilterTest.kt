package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.features.tabgroup.domain.currency.Currency
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrencyFilterTest {
    private val currencies =
        listOf(
            Currency(code = "EUR", name = "Euro", nativeSymbol = "€", decimalDigits = 2),
            Currency(code = "USD", name = "US Dollar", nativeSymbol = "$", decimalDigits = 2),
            Currency(code = "CAD", name = "Canadian Dollar", nativeSymbol = "$", decimalDigits = 2),
            Currency(code = "GBP", name = "British Pound", nativeSymbol = "£", decimalDigits = 2),
        )

    @Test
    fun blankQueryReturnsAllCurrencies() {
        assertEquals(currencies, filterCurrencies(currencies, ""))
        assertEquals(currencies, filterCurrencies(currencies, "   "))
    }

    @Test
    fun queryMatchesCodeCaseInsensitive() {
        val codes = filterCurrencies(currencies, "eur").map { it.code }
        assertContains(codes, "EUR")
    }

    @Test
    fun queryMatchesNameCaseInsensitive() {
        val codes = filterCurrencies(currencies, "dollar").map { it.code }
        assertContains(codes, "USD")
        assertContains(codes, "CAD")
    }

    @Test
    fun queryTrimsWhitespace() {
        val result = filterCurrencies(currencies, "  GBP  ")
        assertEquals(listOf("GBP"), result.map { it.code })
    }

    @Test
    fun unmatchedQueryReturnsEmpty() {
        assertTrue(filterCurrencies(currencies, "xyzzz").isEmpty())
    }
}
