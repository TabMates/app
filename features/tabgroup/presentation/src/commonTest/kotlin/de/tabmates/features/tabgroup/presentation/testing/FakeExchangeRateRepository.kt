package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeExchangeRateRepository(
    initialRates: List<ExchangeRate> = emptyList(),
) : ExchangeRateRepository {
    private val ratesFlow = MutableStateFlow(initialRates)

    fun emit(rates: List<ExchangeRate>) {
        ratesFlow.value = rates
    }

    override fun getExchangeRates(): Flow<List<ExchangeRate>> = ratesFlow

    override fun getExchangeRate(currencyCode: String): Flow<ExchangeRate?> =
        ratesFlow.map { rates -> rates.firstOrNull { it.currencyCode == currencyCode } }

    override suspend fun fetchExchangeRates(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun deleteAllExchangeRates() {
        ratesFlow.value = emptyList()
    }
}
