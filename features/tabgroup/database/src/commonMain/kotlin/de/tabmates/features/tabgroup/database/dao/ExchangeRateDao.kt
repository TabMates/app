package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Upsert
    suspend fun upsertExchangeRates(rates: List<ExchangeRateEntity>)

    @Query("SELECT * FROM exchangerateentity ORDER BY currencyCode ASC")
    fun getExchangeRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchangerateentity WHERE currencyCode = :currencyCode")
    fun getExchangeRateByCode(currencyCode: String): Flow<ExchangeRateEntity?>

    @Query("DELETE FROM exchangerateentity")
    suspend fun deleteAllExchangeRates()

    @Transaction
    suspend fun replaceAllExchangeRates(rates: List<ExchangeRateEntity>) {
        deleteAllExchangeRates()
        if (rates.isNotEmpty()) {
            upsertExchangeRates(rates)
        }
    }
}
