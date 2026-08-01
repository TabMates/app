package de.tabmates.core.data.environment

import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.environment.CustomEnvironment
import de.tabmates.core.domain.environment.EnvironmentRepository
import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.environment.EnvironmentSwitcher
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.sync.LocalDataResetter
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import org.koin.core.annotation.Single

@Single(binds = [EnvironmentSwitcher::class])
class DefaultEnvironmentSwitcher(
    private val environmentRepository: EnvironmentRepository,
    private val environmentProbe: EnvironmentProbe,
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    private val staleSessionStore: StaleSessionStore,
    private val localDataResetter: LocalDataResetter,
    private val appPreferencesRepository: AppPreferencesRepository,
) : EnvironmentSwitcher {
    override suspend fun useCustom(
        httpBaseUrl: String,
        apiKey: String,
    ): EmptyResult<EnvironmentSwitchError> {
        // Where the repository refuses to store a custom environment (web), stop before the probe:
        // going on would wipe the device in `clearPreviousEnvironmentState` for a switch that the
        // repository then silently drops.
        if (!environmentRepository.isSwitchSupported) return Result.Failure(EnvironmentSwitchError.UNREACHABLE)

        if (apiKey.isBlank()) return Result.Failure(EnvironmentSwitchError.MISSING_API_KEY)

        val normalizedUrl =
            when (val normalized = EnvironmentUrls.normalizeHttpBaseUrl(httpBaseUrl)) {
                is Result.Failure -> return Result.Failure(normalized.error)
                is Result.Success -> normalized.data
            }

        // Everything below this line changes the device. Nothing above it did, so a rejected
        // probe leaves the user exactly where they were.
        environmentProbe.probe(normalizedUrl, apiKey).let { probeResult ->
            if (probeResult is Result.Failure) return Result.Failure(probeResult.error)
        }

        environmentRepository.useCustom(CustomEnvironment(httpBaseUrl = normalizedUrl, apiKey = apiKey))
        clearPreviousEnvironmentState()
        return Result.Success(Unit)
    }

    override suspend fun useDefault() {
        // Same reasoning as above: on web the active environment is already the default one, so
        // this would only wipe the device without changing anything.
        if (!environmentRepository.isSwitchSupported) return

        environmentRepository.useDefault()
        clearPreviousEnvironmentState()
    }

    /**
     * Identities, groups and rates all belong to the backend they came from: the same id means a
     * different thing on the next one, and queued outbox writes would replay against a server that
     * never saw the rows they reference.
     */
    private suspend fun clearPreviousEnvironmentState() {
        // Storage before the in-memory cache, not the other way round: `clearAuthTokens()` only
        // empties the cache, and Ktor's `loadTokens` re-primes it from storage on the next call.
        // Clearing the cache first leaves a window in which that re-read still finds the old
        // session and hands the *previous* backend's bearer to the new one.
        sessionStorage.set(null)
        httpClient.clearAuthTokens()
        staleSessionStore.clear()
        localDataResetter.resetLocalData()
        localDataResetter.resetReferenceData()
        appPreferencesRepository.setLastCurrencySync(null)
    }
}
