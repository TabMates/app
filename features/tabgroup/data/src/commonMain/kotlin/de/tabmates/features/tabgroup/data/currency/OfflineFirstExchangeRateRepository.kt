package de.tabmates.features.tabgroup.data.currency

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntities
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.currency.CurrencyService
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExchangeRateRepository::class])
class OfflineFirstExchangeRateRepository(
    private val currencyService: CurrencyService,
    private val database: TabMatesDatabase,
) : ExchangeRateRepository {
    override fun getExchangeRates(): Flow<List<ExchangeRate>> =
        database.exchangeRateDao
            .getExchangeRates()
            .map { rows -> rows.map { it.toDomain() } }

    override fun getExchangeRate(currencyCode: String): Flow<ExchangeRate?> =
        database.exchangeRateDao
            .getExchangeRateByCode(currencyCode)
            .map { it?.toDomain() }

    override suspend fun fetchExchangeRates(): EmptyResult<DataError.Remote> =
        currencyService
            .getExchangeRates()
            .onSuccess { payload ->
                database.exchangeRateDao.replaceAllExchangeRates(payload.toEntities())
            }.asEmptyResult()

    override suspend fun deleteAllExchangeRates() {
        database.exchangeRateDao.deleteAllExchangeRates()
    }
}
