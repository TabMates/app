package de.tabmates.features.tabgroup.data.currency

import de.tabmates.core.data.networking.get
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.map
import de.tabmates.features.tabgroup.data.dto.CurrencyDto
import de.tabmates.features.tabgroup.data.dto.CurrencyRatesDto
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.domain.currency.CurrencyService
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.RatesPayload
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single(binds = [CurrencyService::class])
class KtorCurrencyService(
    private val httpClient: HttpClient,
) : CurrencyService {
    override suspend fun getCurrencies(): Result<List<Currency>, DataError.Remote> =
        httpClient
            .get<List<CurrencyDto>>(route = "/api/currencies")
            .map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getExchangeRates(): Result<RatesPayload, DataError.Remote> =
        httpClient
            .get<CurrencyRatesDto>(route = "/api/currencies/rates")
            .map { it.toDomain() }
}
