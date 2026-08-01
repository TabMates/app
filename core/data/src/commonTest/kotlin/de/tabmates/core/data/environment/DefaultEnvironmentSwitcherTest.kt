package de.tabmates.core.data.environment

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.preferences.AppLanguage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.core.domain.sync.LocalDataResetter
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.testing.environment.FakeEnvironmentRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private fun authInfo(): AuthInfo =
    AuthInfo(
        accessToken = "access",
        refreshToken = "refresh",
        user =
            User(
                id = "user-1",
                email = "lena@example.com",
                username = "lena",
                hasVerifiedEmail = true,
                userType = UserType.REGISTERED,
            ),
    )

private fun staleSession(): StaleSession =
    StaleSession(
        userId = "user-1",
        email = "lena@example.com",
        username = "lena",
        userType = UserType.REGISTERED,
    )

class DefaultEnvironmentSwitcherTest {
    @Test
    fun `a successful switch stores the environment and clears what belonged to the old one`() =
        runTest {
            val fixture = Fixture()

            val result = fixture.switcher.useCustom("https://custom.example.com/", "custom-key")

            assertTrue(result is Result.Success)
            assertEquals("https://custom.example.com", fixture.environmentRepository.current.httpBaseUrl)
            assertEquals("wss://custom.example.com/ws", fixture.environmentRepository.current.wsBaseUrl)
            assertEquals("custom-key", fixture.environmentRepository.current.apiKey)
            assertNull(fixture.sessionStorage.get())
            assertNull(fixture.staleSessionStore.get())
            assertEquals(1, fixture.localDataResetter.resetLocalDataCalls)
            assertEquals(1, fixture.localDataResetter.resetReferenceDataCalls)
            assertEquals(listOf<Instant?>(null), fixture.appPreferences.setLastCurrencySyncCalls)
        }

    @Test
    fun `a rejected probe changes nothing at all`() =
        runTest {
            val fixture = Fixture(probeResult = Result.Failure(EnvironmentSwitchError.KEY_REJECTED))

            val result = fixture.switcher.useCustom("https://custom.example.com", "wrong-key")

            assertEquals(EnvironmentSwitchError.KEY_REJECTED, (result as Result.Failure).error)
            assertEquals("https://default.example.com", fixture.environmentRepository.current.httpBaseUrl)
            assertEquals(authInfo(), fixture.sessionStorage.get())
            assertEquals(0, fixture.localDataResetter.resetLocalDataCalls)
        }

    @Test
    fun `an invalid url is rejected before the probe runs`() =
        runTest {
            val fixture = Fixture()

            val result = fixture.switcher.useCustom("api.example.com", "key")

            assertEquals(EnvironmentSwitchError.INVALID_URL, (result as Result.Failure).error)
            assertEquals(0, fixture.probe.calls)
            assertEquals(0, fixture.localDataResetter.resetLocalDataCalls)
        }

    @Test
    fun `a blank api key is rejected before the probe runs`() =
        runTest {
            val fixture = Fixture()

            val result = fixture.switcher.useCustom("https://custom.example.com", "  ")

            assertEquals(EnvironmentSwitchError.MISSING_API_KEY, (result as Result.Failure).error)
            assertEquals(0, fixture.probe.calls)
        }

    @Test
    fun `going back to the default wipes just as thoroughly`() =
        runTest {
            val fixture = Fixture()
            fixture.switcher.useCustom("https://custom.example.com", "custom-key")

            fixture.switcher.useDefault()

            assertEquals("https://default.example.com", fixture.environmentRepository.current.httpBaseUrl)
            // The custom entry survives so it can be re-activated without retyping the key.
            assertEquals("https://custom.example.com", fixture.environmentRepository.storedCustom?.httpBaseUrl)
            assertEquals(2, fixture.localDataResetter.resetLocalDataCalls)
            assertEquals(2, fixture.localDataResetter.resetReferenceDataCalls)
        }

    @Test
    fun `a platform that cannot switch is refused before anything is touched`() =
        runTest {
            // Web: the repository would drop the write, so probing and wiping would cost the user
            // their local data for a switch that never happens.
            val fixture = Fixture(isSwitchSupported = false)

            val result = fixture.switcher.useCustom("https://custom.example.com", "custom-key")
            fixture.switcher.useDefault()

            assertEquals(EnvironmentSwitchError.UNREACHABLE, (result as Result.Failure).error)
            assertEquals(0, fixture.probe.calls)
            assertEquals(0, fixture.localDataResetter.resetLocalDataCalls)
            assertEquals(authInfo(), fixture.sessionStorage.get())
        }

    private class Fixture(
        probeResult: EmptyResult<EnvironmentSwitchError> = Result.Success(Unit),
        isSwitchSupported: Boolean = true,
    ) {
        val environmentRepository = FakeEnvironmentRepository(isSwitchSupported = isSwitchSupported)
        val probe = FakeEnvironmentProbe(probeResult)
        val sessionStorage = FakeSessionStorage(authInfo())
        val staleSessionStore = FakeStaleSessionStore(staleSession())
        val localDataResetter = RecordingLocalDataResetter()
        val appPreferences = RecordingAppPreferencesRepository()

        val switcher =
            DefaultEnvironmentSwitcher(
                environmentRepository = environmentRepository,
                environmentProbe = probe,
                httpClient = HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
                sessionStorage = sessionStorage,
                staleSessionStore = staleSessionStore,
                localDataResetter = localDataResetter,
                appPreferencesRepository = appPreferences,
            )
    }

    private class FakeEnvironmentProbe(
        private val result: EmptyResult<EnvironmentSwitchError>,
    ) : EnvironmentProbe {
        var calls: Int = 0
            private set

        override suspend fun probe(
            httpBaseUrl: String,
            apiKey: String,
        ): EmptyResult<EnvironmentSwitchError> {
            calls += 1
            return result
        }
    }

    private class FakeSessionStorage(
        initial: AuthInfo?,
    ) : SessionStorage {
        private val state = MutableStateFlow(initial)

        override val authState: StateFlow<AuthInfo?> = state

        override fun get(): AuthInfo? = state.value

        override fun set(info: AuthInfo?) {
            state.value = info
        }
    }

    private class FakeStaleSessionStore(
        initial: StaleSession?,
    ) : StaleSessionStore {
        private val internalState = MutableStateFlow(initial)

        override val state: StateFlow<StaleSession?> = internalState

        override fun get(): StaleSession? = internalState.value

        override fun set(session: StaleSession?) {
            internalState.value = session
        }

        override fun clear() = set(null)
    }

    private class RecordingLocalDataResetter : LocalDataResetter {
        var resetLocalDataCalls: Int = 0
            private set
        var resetReferenceDataCalls: Int = 0
            private set

        override suspend fun resetLocalData() {
            resetLocalDataCalls += 1
        }

        override suspend fun resetReferenceData() {
            resetReferenceDataCalls += 1
        }
    }

    private class RecordingAppPreferencesRepository : AppPreferencesRepository {
        val setLastCurrencySyncCalls: MutableList<Instant?> = mutableListOf()

        override fun themeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

        override suspend fun setThemeMode(mode: ThemeMode) = Unit

        override fun notificationsEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit

        override fun appLanguage(): Flow<AppLanguage> = flowOf(AppLanguage.SYSTEM)

        override suspend fun setAppLanguage(language: AppLanguage) = Unit

        override fun androidAppPromoSnoozedUntil(): Flow<Instant?> = flowOf(null)

        override suspend fun snoozeAndroidAppPromo(until: Instant) = Unit

        override suspend fun lastCurrencySync(): Instant? = null

        override suspend fun setLastCurrencySync(instant: Instant?) {
            setLastCurrencySyncCalls += instant
        }
    }
}
