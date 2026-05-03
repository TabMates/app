package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.features.tabgroup.domain.currency.Currency
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository

class FakeCurrencyRepository(
    var currencies: List<Currency> = DEFAULT_CURRENCIES,
) : CurrencyRepository {
    var getCurrenciesCalls: Int = 0
        private set

    override suspend fun getCurrencies(): List<Currency> {
        getCurrenciesCalls += 1
        return currencies
    }

    companion object {
        val DEFAULT_CURRENCIES =
            listOf(
                Currency(code = "EUR", name = "Euro", nativeSymbol = "€", decimalDigits = 2),
                Currency(code = "USD", name = "US Dollar", nativeSymbol = "$", decimalDigits = 2),
                Currency(code = "GBP", name = "British Pound", nativeSymbol = "£", decimalDigits = 2),
            )
    }
}
