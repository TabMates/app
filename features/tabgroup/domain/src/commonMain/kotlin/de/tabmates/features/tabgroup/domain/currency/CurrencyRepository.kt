package de.tabmates.features.tabgroup.domain.currency

interface CurrencyRepository {
    suspend fun getCurrencies(): List<Currency>
}
