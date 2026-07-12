package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.features.tabgroup.domain.models.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class CurrencyPickerStateTest {
    private val currencies =
        listOf(
            currency("EUR", "Euro", "€"),
            currency("USD", "US Dollar", "$"),
        )

    @Test
    fun omittingRateParamsYieldsNoBaseAndNoRates() {
        val state =
            buildCurrencyPickerState(
                currencies = currencies,
                recentCodes = emptyList(),
                selectedCode = "EUR",
                query = "",
            )
        assertEquals("", state.baseCurrencyCode)
        assertEquals(emptyMap(), state.ratesByCurrency)
        assertNull(state.ratesLastUpdatedAt)
    }

    @Test
    fun suppliedBaseAndRatesAreCarriedThrough() {
        val lastUpdatedAt = Instant.fromEpochMilliseconds(1_752_000_000_000)
        val rates = mapOf("USD" to 1.0, "EUR" to 0.92)
        val state =
            buildCurrencyPickerState(
                currencies = currencies,
                recentCodes = listOf("EUR"),
                selectedCode = "USD",
                query = "",
                baseCurrencyCode = "EUR",
                ratesByCurrency = rates,
                ratesLastUpdatedAt = lastUpdatedAt,
            )
        assertEquals("EUR", state.baseCurrencyCode)
        assertEquals(rates, state.ratesByCurrency)
        assertEquals(lastUpdatedAt, state.ratesLastUpdatedAt)
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
