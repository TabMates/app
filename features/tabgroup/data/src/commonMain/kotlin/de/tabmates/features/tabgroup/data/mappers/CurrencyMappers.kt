package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.CurrencyDto
import de.tabmates.features.tabgroup.data.dto.CurrencyRatesDto
import de.tabmates.features.tabgroup.database.entities.CurrencyEntity
import de.tabmates.features.tabgroup.database.entities.ExchangeRateEntity
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import de.tabmates.features.tabgroup.domain.models.RatesPayload
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal const val DEFAULT_BASE_CURRENCY = "USD"

private val countriesJson = Json { ignoreUnknownKeys = true }

fun CurrencyDto.toDomain(): Currency =
    Currency(
        code = code,
        name = name,
        nativeSymbol = symbolNative,
        decimalDigits = decimalDigits,
        type = type,
        countries = countries,
    )

fun Currency.toEntity(): CurrencyEntity =
    CurrencyEntity(
        code = code,
        name = name,
        nativeSymbol = nativeSymbol,
        decimalDigits = decimalDigits,
        type = type,
        countries = countriesJson.encodeToString(countries),
    )

fun CurrencyEntity.toDomain(): Currency =
    Currency(
        code = code,
        name = name,
        nativeSymbol = nativeSymbol,
        decimalDigits = decimalDigits,
        type = type,
        countries =
            if (countries.isBlank()) {
                emptyList()
            } else {
                countriesJson.decodeFromString(countries)
            },
    )

fun CurrencyRatesDto.toDomain(baseCurrency: String = DEFAULT_BASE_CURRENCY): RatesPayload =
    RatesPayload(
        baseCurrency = baseCurrency,
        lastUpdatedAt = lastUpdatedAt,
        rates = rates,
    )

fun RatesPayload.toEntities(): List<ExchangeRateEntity> {
    val updatedMs = lastUpdatedAt.toEpochMilliseconds()
    return rates.map { (code, rate) ->
        ExchangeRateEntity(
            currencyCode = code,
            rateToBase = rate,
            baseCurrency = baseCurrency,
            lastUpdatedAtEpochMs = updatedMs,
        )
    }
}

fun ExchangeRateEntity.toDomain(): ExchangeRate =
    ExchangeRate(
        currencyCode = currencyCode,
        rateToBase = rateToBase,
        baseCurrency = baseCurrency,
        lastUpdatedAt = Instant.fromEpochMilliseconds(lastUpdatedAtEpochMs),
    )
