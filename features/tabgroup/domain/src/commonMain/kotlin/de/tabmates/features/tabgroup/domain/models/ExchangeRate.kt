package de.tabmates.features.tabgroup.domain.models

import kotlin.time.Instant

data class ExchangeRate(
    val currencyCode: String,
    val rateToBase: Double,
    val baseCurrency: String,
    val lastUpdatedAt: Instant,
)
