package de.tabmates.features.tabgroup.domain.models

import kotlin.time.Instant

data class RatesPayload(
    val baseCurrency: String,
    val lastUpdatedAt: Instant,
    val rates: Map<String, Double>,
)
