package de.tabmates.composeapp.sync

import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import eu.anifantakis.lib.ksafe.invoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Single
class CurrencySyncCoordinator(
    sessionStorage: SessionStorage,
    @Named("prefs") prefs: KSafe,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var lastCurrencySyncEpochMs: Long by prefs(0L, key = "lastCurrencySync", mode = KSafeWriteMode.Plain)

    init {
        sessionStorage.authState
            .map { it != null }
            .distinctUntilChanged()
            .onEach { loggedIn -> if (loggedIn) refresh() }
            .launchIn(scope)
    }

    private suspend fun refresh() {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastCurrencySyncEpochMs < SYNC_INTERVAL.inWholeMilliseconds) return

        val (currenciesResult, ratesResult) = coroutineScope {
            val currencies = async { currencyRepository.fetchCurrencies() }
            val rates = async { exchangeRateRepository.fetchExchangeRates() }
            currencies.await() to rates.await()
        }

        if (currenciesResult is Result.Success && ratesResult is Result.Success) {
            lastCurrencySyncEpochMs = now
        }
    }

    private companion object {
        private val SYNC_INTERVAL = 1.days
    }
}
