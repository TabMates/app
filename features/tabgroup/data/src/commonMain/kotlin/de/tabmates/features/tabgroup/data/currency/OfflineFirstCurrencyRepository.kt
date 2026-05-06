package de.tabmates.features.tabgroup.data.currency

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.CurrencyService
import de.tabmates.features.tabgroup.domain.models.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [CurrencyRepository::class])
class OfflineFirstCurrencyRepository(
    private val currencyService: CurrencyService,
    private val database: TabMatesDatabase,
) : CurrencyRepository {
    override fun getCurrencies(): Flow<List<Currency>> =
        database.currencyDao
            .getCurrencies()
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun fetchCurrencies(): EmptyResult<DataError.Remote> =
        currencyService
            .getCurrencies()
            .onSuccess { currencies ->
                val fiat = currencies.filter { it.type == FIAT_TYPE }
                database.currencyDao.replaceAllCurrencies(fiat.map { it.toEntity() })
            }.asEmptyResult()

    override suspend fun deleteAllCurrencies() {
        database.currencyDao.deleteAllCurrencies()
    }

    private companion object {
        private const val FIAT_TYPE = "fiat"
    }
}
