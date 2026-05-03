package de.tabmates.features.tabgroup.data.currency

import de.tabmates.features.tabgroup.domain.currency.Currency
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.annotation.Single
import tabmatesapp.features.tabgroup.data.generated.resources.Res

@Single(binds = [CurrencyRepository::class])
class LocalCurrencyRepository : CurrencyRepository {
    private val mutex = Mutex()
    private var cached: List<Currency>? = null

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getCurrencies(): List<Currency> {
        cached?.let { return it }
        return mutex.withLock {
            cached?.let { return@withLock it }
            val bytes = Res.readBytes("files/currencies.json")
            val parsed =
                json
                    .decodeFromString<CurrenciesFile>(bytes.decodeToString())
                    .data
                    .values
                    .filter { it.type == TYPE_FIAT }
                    .map { it.toDomain() }
                    .sortedBy { it.code }
            parsed.also { cached = it }
        }
    }

    private companion object {
        private const val TYPE_FIAT = "fiat"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class CurrenciesFile(
    val data: Map<String, CurrencyEntry>,
)

@Serializable
private data class CurrencyEntry(
    val code: String,
    val name: String,
    @SerialName("symbol_native") val symbolNative: String,
    @SerialName("decimal_digits") val decimalDigits: Int,
    val type: String,
) {
    fun toDomain(): Currency =
        Currency(
            code = code,
            name = name,
            nativeSymbol = symbolNative,
            decimalDigits = decimalDigits,
        )
}
