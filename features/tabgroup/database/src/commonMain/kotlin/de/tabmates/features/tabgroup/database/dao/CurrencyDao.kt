package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Upsert
    suspend fun upsertCurrencies(currencies: List<CurrencyEntity>)

    @Query("SELECT * FROM currencyentity ORDER BY code ASC")
    fun getCurrencies(): Flow<List<CurrencyEntity>>

    @Query("DELETE FROM currencyentity")
    suspend fun deleteAllCurrencies()

    @Transaction
    suspend fun replaceAllCurrencies(currencies: List<CurrencyEntity>) {
        deleteAllCurrencies()
        if (currencies.isNotEmpty()) {
            upsertCurrencies(currencies)
        }
    }
}
