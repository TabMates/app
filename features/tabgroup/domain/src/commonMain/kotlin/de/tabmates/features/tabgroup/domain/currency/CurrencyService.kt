package de.tabmates.features.tabgroup.domain.currency

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.RatesPayload

interface CurrencyService {
    suspend fun getCurrencies(): Result<List<Currency>, DataError.Remote>

    suspend fun getExchangeRates(): Result<RatesPayload, DataError.Remote>
}
