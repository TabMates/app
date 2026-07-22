package de.tabmates.composeapp.lock

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAvailability
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.biometric.BiometricResult
import de.tabmates.core.domain.preferences.AppLanguage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val promptStrings = BiometricPromptStrings("title", "subtitle", "cancel")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAppPreferencesRepository(
        biometricEnabled: Boolean,
    ) : AppPreferencesRepository {
        val biometric = MutableStateFlow(biometricEnabled)

        override fun themeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

        override suspend fun setThemeMode(mode: ThemeMode) = Unit

        override fun notificationsEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit

        override fun appLanguage(): Flow<AppLanguage> = flowOf(AppLanguage.SYSTEM)

        override suspend fun setAppLanguage(language: AppLanguage) = Unit

        override fun biometricUnlockEnabled(): Flow<Boolean> = biometric

        override suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
            biometric.value = enabled
        }
    }

    private class FakeSessionStorage(
        info: AuthInfo?,
    ) : SessionStorage {
        private val state = MutableStateFlow(info)
        override val authState = state

        override fun get(): AuthInfo? = state.value

        override fun set(info: AuthInfo?) {
            state.value = info
        }
    }

    private class FakeBiometricAuthenticator(
        var result: BiometricResult = BiometricResult.Success,
    ) : BiometricAuthenticator {
        var calls = 0

        override fun availability(): BiometricAvailability = BiometricAvailability.AVAILABLE

        override suspend fun authenticate(strings: BiometricPromptStrings): BiometricResult {
            calls++
            return result
        }
    }

    private fun authInfo(): AuthInfo =
        AuthInfo(
            accessToken = "access",
            refreshToken = "refresh",
            user =
                User(
                    id = "id",
                    email = "user@example.com",
                    username = "user",
                    hasVerifiedEmail = true,
                    userType = UserType.REGISTERED,
                ),
        )

    private fun viewModel(
        enabled: Boolean,
        session: FakeSessionStorage,
        bio: FakeBiometricAuthenticator = FakeBiometricAuthenticator(),
    ) = AppLockViewModel(
        appPreferencesRepository = FakeAppPreferencesRepository(enabled),
        sessionStorage = session,
        controller = AppLockController(),
        biometricAuthenticator = bio,
    )

    @Test
    fun disabled_isUnlocked() =
        runTest(testDispatcher) {
            val vm = viewModel(enabled = false, session = FakeSessionStorage(authInfo()))
            advanceUntilIdle()
            assertEquals(AppLockUiState.UNLOCKED, vm.uiState.value)
        }

    @Test
    fun enabledAndLoggedIn_isLockedUntilAuthenticated() =
        runTest(testDispatcher) {
            val bio = FakeBiometricAuthenticator(result = BiometricResult.Success)
            val vm = viewModel(enabled = true, session = FakeSessionStorage(authInfo()), bio = bio)
            advanceUntilIdle()
            assertEquals(AppLockUiState.LOCKED, vm.uiState.value)

            vm.authenticate(promptStrings)
            advanceUntilIdle()

            assertEquals(AppLockUiState.UNLOCKED, vm.uiState.value)
            assertEquals(1, bio.calls)
        }

    @Test
    fun enabledButLoggedOut_isUnlocked() =
        runTest(testDispatcher) {
            val vm = viewModel(enabled = true, session = FakeSessionStorage(null))
            advanceUntilIdle()
            assertEquals(AppLockUiState.UNLOCKED, vm.uiState.value)
        }

    @Test
    fun authenticationError_staysLockedAndFlagsError() =
        runTest(testDispatcher) {
            val bio = FakeBiometricAuthenticator(result = BiometricResult.Error("boom"))
            val vm = viewModel(enabled = true, session = FakeSessionStorage(authInfo()), bio = bio)
            advanceUntilIdle()

            vm.authenticate(promptStrings)
            advanceUntilIdle()

            assertEquals(AppLockUiState.LOCKED, vm.uiState.value)
            assertTrue(vm.authError.value)
        }

    @Test
    fun cancelledAuthentication_staysLockedWithoutError() =
        runTest(testDispatcher) {
            val bio = FakeBiometricAuthenticator(result = BiometricResult.Cancelled)
            val vm = viewModel(enabled = true, session = FakeSessionStorage(authInfo()), bio = bio)
            advanceUntilIdle()

            vm.authenticate(promptStrings)
            advanceUntilIdle()

            assertEquals(AppLockUiState.LOCKED, vm.uiState.value)
            assertTrue(!vm.authError.value)
        }

    @Test
    fun onSignedIn_unlocks() =
        runTest(testDispatcher) {
            val vm = viewModel(enabled = true, session = FakeSessionStorage(authInfo()))
            advanceUntilIdle()
            assertEquals(AppLockUiState.LOCKED, vm.uiState.value)

            vm.onSignedIn()
            advanceUntilIdle()

            assertEquals(AppLockUiState.UNLOCKED, vm.uiState.value)
        }

    @Test
    fun signOut_clearsSessionAndUnlocks() =
        runTest(testDispatcher) {
            val session = FakeSessionStorage(authInfo())
            val vm = viewModel(enabled = true, session = session)
            advanceUntilIdle()

            vm.signOut()
            advanceUntilIdle()

            assertNull(session.get())
            assertEquals(AppLockUiState.UNLOCKED, vm.uiState.value)
        }
}
