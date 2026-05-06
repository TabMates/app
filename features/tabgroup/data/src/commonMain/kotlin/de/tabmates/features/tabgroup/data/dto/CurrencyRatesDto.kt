package de.tabmates.features.tabgroup.data.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CurrencyRatesDto(
    val lastUpdatedAt: Instant,
    val rates: Map<String, Double>,
)
