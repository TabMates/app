package de.tabmates.features.tabgroup.domain.currency

data class Currency(
    val code: String,
    val name: String,
    val nativeSymbol: String,
    val decimalDigits: Int,
)
