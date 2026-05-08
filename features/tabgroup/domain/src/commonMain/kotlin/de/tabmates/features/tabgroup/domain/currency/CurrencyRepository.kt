package de.tabmates.features.tabgroup.domain.currency

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.features.tabgroup.domain.models.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    fun getCurrencies(): Flow<List<Currency>>

    suspend fun fetchCurrencies(): EmptyResult<DataError.Remote>

    suspend fun deleteAllCurrencies()
}
