package de.tabmates.features.tabgroup.domain.currency

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun getExchangeRates(): Flow<List<ExchangeRate>>

    fun getExchangeRate(currencyCode: String): Flow<ExchangeRate?>

    suspend fun fetchExchangeRates(): EmptyResult<DataError.Remote>

    suspend fun deleteAllExchangeRates()
}
