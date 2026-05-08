package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class ExchangeRateEntity(
    @PrimaryKey
    val currencyCode: String,
    val rateToBase: Double,
    val baseCurrency: String,
    val lastUpdatedAtEpochMs: Long,
)
