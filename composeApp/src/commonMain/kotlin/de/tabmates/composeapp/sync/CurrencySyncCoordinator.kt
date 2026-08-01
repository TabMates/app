package de.tabmates.composeapp.sync

import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.currency.ExchangeRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Single
class CurrencySyncCoordinator(
    sessionStorage: SessionStorage,
    // The stamp lives in the preferences repository rather than in a KSafe delegate here: the
    // environment switch has to clear it (the next backend has its own currencies) and must not
    // reach into this class to do it.
    private val appPreferencesRepository: AppPreferencesRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        sessionStorage.authState
            .map { it != null }
            .distinctUntilChanged()
            .onEach { loggedIn -> if (loggedIn) refresh() }
            .launchIn(scope)
    }

    private suspend fun refresh() {
        val now = Clock.System.now()
        val lastSync = appPreferencesRepository.lastCurrencySync()
        if (lastSync != null && now - lastSync < SYNC_INTERVAL) return

        val (currenciesResult, ratesResult) = coroutineScope {
            val currencies = async { currencyRepository.fetchCurrencies() }
            val rates = async { exchangeRateRepository.fetchExchangeRates() }
            currencies.await() to rates.await()
        }

        if (currenciesResult is Result.Success && ratesResult is Result.Success) {
            appPreferencesRepository.setLastCurrencySync(now)
        }
    }

    private companion object {
        private val SYNC_INTERVAL = 1.days
    }
}
