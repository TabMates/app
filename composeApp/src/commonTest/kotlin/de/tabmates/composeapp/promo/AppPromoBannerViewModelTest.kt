package de.tabmates.composeapp.promo

import app.cash.turbine.test
import de.tabmates.core.domain.preferences.AppLanguage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppPromoBannerViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAppPreferencesRepository(
        snoozedUntil: Instant? = null,
    ) : AppPreferencesRepository {
        val promoSnoozedUntil = MutableStateFlow(snoozedUntil)

        override fun androidAppPromoSnoozedUntil(): Flow<Instant?> = promoSnoozedUntil

        override suspend fun snoozeAndroidAppPromo(until: Instant) {
            promoSnoozedUntil.value = until
        }

        override fun themeMode(): Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)

        override suspend fun setThemeMode(mode: ThemeMode) = Unit

        override fun notificationsEnabled(): Flow<Boolean> = MutableStateFlow(true)

        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit

        override fun appLanguage(): Flow<AppLanguage> = MutableStateFlow(AppLanguage.SYSTEM)

        override suspend fun setAppLanguage(language: AppLanguage) = Unit
    }

    @Test
    fun visibleWhenNeverSnoozed() =
        runTest(testDispatcher) {
            val viewModel = AppPromoBannerViewModel(FakeAppPreferencesRepository())

            viewModel.state.test {
                assertTrue(awaitItem().isVisible)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun hiddenWhileSnoozeIsInTheFuture() =
        runTest(testDispatcher) {
            val repository = FakeAppPreferencesRepository(snoozedUntil = Clock.System.now() + 1.days)
            val viewModel = AppPromoBannerViewModel(repository)

            viewModel.state.test {
                assertFalse(awaitItem().isVisible)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun visibleAgainOnceSnoozeHasPassed() =
        runTest(testDispatcher) {
            val repository = FakeAppPreferencesRepository(snoozedUntil = Clock.System.now() - 1.minutes)
            val viewModel = AppPromoBannerViewModel(repository)

            viewModel.state.test {
                assertTrue(awaitItem().isVisible)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun dismissSnoozesAndHidesTheBanner() =
        runTest(testDispatcher) {
            val repository = FakeAppPreferencesRepository()
            val viewModel = AppPromoBannerViewModel(repository)

            viewModel.state.test {
                assertTrue(awaitItem().isVisible)

                val before = Clock.System.now()
                viewModel.onDismiss()

                assertFalse(awaitItem().isVisible)
                val storedUntil = assertNotNull(repository.promoSnoozedUntil.value)
                val expected = before + AppPromoBannerViewModel.SNOOZE_DURATION
                assertTrue(storedUntil in (expected - 1.seconds)..(expected + 1.seconds))
                cancelAndConsumeRemainingEvents()
            }
        }
}
