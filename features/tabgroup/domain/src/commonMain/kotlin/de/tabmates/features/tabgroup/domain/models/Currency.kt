package de.tabmates.features.tabgroup.domain.models

data class Currency(
    val code: String,
    val name: String,
    val nativeSymbol: String,
    val decimalDigits: Int,
    val type: String,
    val countries: List<String>,
)
