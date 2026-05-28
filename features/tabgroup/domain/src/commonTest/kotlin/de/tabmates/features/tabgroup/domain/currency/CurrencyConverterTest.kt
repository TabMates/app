package de.tabmates.features.tabgroup.domain.currency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrencyConverterTest {
    // rate value = units of that currency per 1 USD (provider base is USD).
    private val rates = mapOf("USD" to 1.0, "EUR" to 0.92, "GBP" to 0.80)

    @Test
    fun sameCurrencyReturnsAmountUnchanged() {
        assertEquals(42.0, CurrencyConverter.convert(42.0, "EUR", "EUR", rates))
    }

    @Test
    fun convertsToUsdBase() {
        // 92 EUR / 0.92 = 100 USD
        assertEquals(100.0, CurrencyConverter.convert(92.0, "EUR", "USD", rates)!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun convertsBetweenTwoNonBaseCurrencies() {
        // 92 EUR -> 100 USD -> 80 GBP
        assertEquals(80.0, CurrencyConverter.convert(92.0, "EUR", "GBP", rates)!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun missingFromRateReturnsNull() {
        assertNull(CurrencyConverter.convert(10.0, "JPY", "EUR", rates))
    }

    @Test
    fun missingToRateReturnsNull() {
        assertNull(CurrencyConverter.convert(10.0, "EUR", "JPY", rates))
    }

    @Test
    fun zeroFromRateReturnsNull() {
        assertNull(CurrencyConverter.convert(10.0, "EUR", "USD", mapOf("EUR" to 0.0, "USD" to 1.0)))
    }

    @Test
    fun factorToBaseConvertsOneUnit() {
        val conversion = CurrencyConversion(baseCurrency = "EUR", rates = rates)
        // 1 GBP -> (1/0.80) USD -> * 0.92 EUR = 1.15 EUR
        assertEquals(1.15, conversion.factorToBase("GBP")!!, absoluteTolerance = 1e-9)
        assertEquals(1.0, conversion.factorToBase("EUR")!!, absoluteTolerance = 1e-9)
        assertNull(conversion.factorToBase("JPY"))
    }
}
