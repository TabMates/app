package de.tabmates.features.tabgroup.domain.currency

import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

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

    @Test
    fun factorForPrefersLockedRateOverLiveRates() {
        val conversion = CurrencyConversion(baseCurrency = "EUR", rates = rates)
        // Live table says 1 USD = 0.92 EUR, but the entry locked 0.85 at creation.
        val entry = usdExpense(exchangeRate = 0.85)
        assertEquals(0.85, conversion.factorFor(entry)!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun factorForFallsBackToLiveRateWithoutLockedRate() {
        val conversion = CurrencyConversion(baseCurrency = "EUR", rates = rates)
        assertEquals(0.92, conversion.factorFor(usdExpense(exchangeRate = null))!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun factorForNullWhenNoLockedRateAndUnknownCurrency() {
        val conversion = CurrencyConversion(baseCurrency = "EUR", rates = mapOf("EUR" to 0.92))
        assertNull(conversion.factorFor(usdExpense(exchangeRate = null)))
    }

    @Test
    fun factorForNullConversionIsOneEvenWithLockedRate() {
        // Single-currency mode ignores rates entirely, locked or live.
        val conversion: CurrencyConversion? = null
        assertEquals(1.0, conversion.factorFor(usdExpense(exchangeRate = 0.85)))
    }

    private fun usdExpense(exchangeRate: Double?): TabEntry.Expense =
        TabEntry.Expense(
            tabEntryId = "e1",
            groupId = "g",
            title = "",
            description = "",
            amount = 100.0,
            currencyCode = "USD",
            exchangeRate = exchangeRate,
            creatorId = "a",
            paidByUserId = "a",
            entryDate = LocalDate.parse("1970-01-01"),
            createdAt = Instant.fromEpochMilliseconds(0),
            lastModifiedAt = Instant.fromEpochMilliseconds(0),
            lastModifiedByUserId = "a",
            version = 0,
            deletedAt = null,
            deletedByUserId = null,
            splits = emptyList(),
        )
}
