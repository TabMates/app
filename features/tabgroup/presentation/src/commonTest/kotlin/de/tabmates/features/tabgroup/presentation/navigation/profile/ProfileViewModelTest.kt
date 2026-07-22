package de.tabmates.features.tabgroup.presentation.navigation.profile

import app.cash.turbine.test
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAvailability
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.biometric.BiometricResult
import de.tabmates.core.domain.preferences.AppLanguage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.features.authentication.testing.FakeAuthService
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import de.tabmates.features.notifications.domain.PushNotificationController
import de.tabmates.features.tabgroup.presentation.testing.FakeSessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
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
        biometricEnabled: Boolean = false,
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

    private class FakeNotificationPermissionController : NotificationPermissionController {
        private val _status = MutableStateFlow(NotificationPermissionStatus.GRANTED)
        override val status: StateFlow<NotificationPermissionStatus> = _status.asStateFlow()

        override suspend fun refresh() = Unit

        override fun openSettings() = Unit
    }

    private class FakePushNotificationController : PushNotificationController {
        override fun start() = Unit

        override suspend fun refreshRegistration() = Unit

        override suspend fun stop() = Unit
    }

    private class FakeBiometricAuthenticator(
        var result: BiometricResult = BiometricResult.Success,
        var availability: BiometricAvailability = BiometricAvailability.AVAILABLE,
    ) : BiometricAuthenticator {
        var calls = 0

        override fun availability(): BiometricAvailability = availability

        override suspend fun authenticate(strings: BiometricPromptStrings): BiometricResult {
            calls++
            return result
        }
    }

    private fun viewModel(
        prefs: FakeAppPreferencesRepository = FakeAppPreferencesRepository(),
        bio: FakeBiometricAuthenticator = FakeBiometricAuthenticator(),
    ) = ProfileViewModel(
        sessionStorage = FakeSessionStorage(),
        appPreferencesRepository = prefs,
        authService = FakeAuthService(),
        notificationPermissionController = FakeNotificationPermissionController(),
        pushNotificationController = FakePushNotificationController(),
        biometricAuthenticator = bio,
    )

    @Test
    fun enablingBiometric_authenticatesThenPersists() =
        runTest(testDispatcher) {
            val prefs = FakeAppPreferencesRepository(biometricEnabled = false)
            val bio = FakeBiometricAuthenticator(result = BiometricResult.Success)
            val vm = viewModel(prefs = prefs, bio = bio)

            vm.onBiometricUnlockToggle(enabled = true, strings = promptStrings)
            advanceUntilIdle()

            assertEquals(1, bio.calls)
            assertTrue(prefs.biometric.value)
        }

    @Test
    fun enablingBiometric_cancelled_doesNotPersist() =
        runTest(testDispatcher) {
            val prefs = FakeAppPreferencesRepository(biometricEnabled = false)
            val bio = FakeBiometricAuthenticator(result = BiometricResult.Cancelled)
            val vm = viewModel(prefs = prefs, bio = bio)

            vm.onBiometricUnlockToggle(enabled = true, strings = promptStrings)
            advanceUntilIdle()

            assertFalse(prefs.biometric.value)
        }

    @Test
    fun enablingBiometric_error_emitsEventAndDoesNotPersist() =
        runTest(testDispatcher) {
            val prefs = FakeAppPreferencesRepository(biometricEnabled = false)
            val bio = FakeBiometricAuthenticator(result = BiometricResult.Error("boom"))
            val vm = viewModel(prefs = prefs, bio = bio)

            vm.events.test {
                vm.onBiometricUnlockToggle(enabled = true, strings = promptStrings)
                assertIs<ProfileEvent.Error>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
            assertFalse(prefs.biometric.value)
        }

    @Test
    fun disablingBiometric_persistsWithoutAuthenticating() =
        runTest(testDispatcher) {
            val prefs = FakeAppPreferencesRepository(biometricEnabled = true)
            val bio = FakeBiometricAuthenticator()
            val vm = viewModel(prefs = prefs, bio = bio)

            vm.onBiometricUnlockToggle(enabled = false, strings = promptStrings)
            advanceUntilIdle()

            assertEquals(0, bio.calls)
            assertFalse(prefs.biometric.value)
        }

    @Test
    fun state_reflectsBiometricAvailability() =
        runTest(testDispatcher) {
            val bio = FakeBiometricAuthenticator(availability = BiometricAvailability.NO_HARDWARE)
            val vm = viewModel(bio = bio)

            vm.state.test {
                var state = awaitItem()
                while (state.isLoading) state = awaitItem()
                assertFalse(state.biometricSupported)
                assertFalse(state.biometricAvailable)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun state_supportedWhenEnrolled() =
        runTest(testDispatcher) {
            val bio = FakeBiometricAuthenticator(availability = BiometricAvailability.AVAILABLE)
            val vm = viewModel(bio = bio)

            vm.state.test {
                var state = awaitItem()
                while (state.isLoading) state = awaitItem()
                assertTrue(state.biometricSupported)
                assertTrue(state.biometricAvailable)
                cancelAndConsumeRemainingEvents()
            }
        }
}
