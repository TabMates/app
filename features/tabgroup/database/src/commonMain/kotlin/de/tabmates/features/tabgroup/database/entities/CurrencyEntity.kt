package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class CurrencyEntity(
    @PrimaryKey
    val code: String,
    val name: String,
    val nativeSymbol: String,
    val decimalDigits: Int,
    val type: String,
    val countries: String,
)
