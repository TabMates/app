package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.models.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCurrencyRepository(
    initialCurrencies: List<Currency> = DEFAULT_CURRENCIES,
) : CurrencyRepository {
    private val currenciesFlow = MutableStateFlow(initialCurrencies)

    var fetchCurrenciesCalls: Int = 0
        private set

    var fetchResult: EmptyResult<DataError.Remote> = Result.Success(Unit)

    fun emit(currencies: List<Currency>) {
        currenciesFlow.value = currencies
    }

    override fun getCurrencies(): Flow<List<Currency>> = currenciesFlow

    override suspend fun fetchCurrencies(): EmptyResult<DataError.Remote> {
        fetchCurrenciesCalls += 1
        return fetchResult
    }

    override suspend fun deleteAllCurrencies() {
        currenciesFlow.value = emptyList()
    }

    companion object {
        val DEFAULT_CURRENCIES =
            listOf(
                Currency(
                    code = "EUR",
                    name = "Euro",
                    nativeSymbol = "€",
                    decimalDigits = 2,
                    type = "fiat",
                    countries = emptyList(),
                ),
                Currency(
                    code = "USD",
                    name = "US Dollar",
                    nativeSymbol = "$",
                    decimalDigits = 2,
                    type = "fiat",
                    countries = emptyList(),
                ),
                Currency(
                    code = "GBP",
                    name = "British Pound",
                    nativeSymbol = "£",
                    decimalDigits = 2,
                    type = "fiat",
                    countries = emptyList(),
                ),
            )
    }
}
