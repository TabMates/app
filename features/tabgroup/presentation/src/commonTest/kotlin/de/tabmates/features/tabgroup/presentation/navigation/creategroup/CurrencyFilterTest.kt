package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.features.tabgroup.domain.models.Currency
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrencyFilterTest {
    private val currencies =
        listOf(
            currency("EUR", "Euro", "€"),
            currency("USD", "US Dollar", "$"),
            currency("CAD", "Canadian Dollar", "$"),
            currency("GBP", "British Pound", "£"),
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

    private fun currency(
        code: String,
        name: String,
        nativeSymbol: String,
    ) = Currency(
        code = code,
        name = name,
        nativeSymbol = nativeSymbol,
        decimalDigits = 2,
        type = "fiat",
        countries = emptyList(),
    )
}
