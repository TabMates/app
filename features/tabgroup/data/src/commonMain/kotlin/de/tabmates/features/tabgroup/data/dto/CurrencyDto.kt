package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyDto(
    val code: String,
    val name: String,
    val namePlural: String,
    val symbol: String,
    val symbolNative: String,
    val decimalDigits: Int,
    val rounding: Int,
    val type: String,
    val countries: List<String>,
)
