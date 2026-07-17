package de.tabmates.composeapp.connectivity

import app.cash.turbine.test
import de.tabmates.core.presentation.util.RelativeTimeSpan
import de.tabmates.features.tabgroup.domain.sync.ConnectionStatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityBannerViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeConnectionStatusRepository(
        initiallyConnected: Boolean = true,
        lastContact: Instant? = null,
    ) : ConnectionStatusRepository {
        override val isConnected = MutableStateFlow(initiallyConnected)
        override val lastServerContactAt = MutableStateFlow(lastContact)
    }

    @Test
    fun hiddenWhileConnected() =
        runTest(testDispatcher) {
            val viewModel = ConnectivityBannerViewModel(FakeConnectionStatusRepository())

            viewModel.state.test {
                val state = awaitItem()
                assertFalse(state.isVisible)
                assertNull(state.lastSyncedSpan)
                expectNoEvents()
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun shownOnlyAfterGracePeriodOnceDisconnected() =
        runTest(testDispatcher) {
            val repository = FakeConnectionStatusRepository(initiallyConnected = true)
            val viewModel = ConnectivityBannerViewModel(repository)

            viewModel.state.test {
                assertFalse(awaitItem().isVisible)

                repository.isConnected.value = false
                advanceTimeBy(ConnectivityBannerViewModel.GRACE_PERIOD - 1.seconds)
                expectNoEvents()

                advanceTimeBy(2.seconds)
                assertTrue(awaitItem().isVisible)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun hiddenImmediatelyOnReconnect() =
        runTest(testDispatcher) {
            val repository = FakeConnectionStatusRepository(initiallyConnected = true)
            val viewModel = ConnectivityBannerViewModel(repository)

            viewModel.state.test {
                assertFalse(awaitItem().isVisible)

                repository.isConnected.value = false
                advanceTimeBy(ConnectivityBannerViewModel.GRACE_PERIOD + 1.seconds)
                assertTrue(awaitItem().isVisible)

                repository.isConnected.value = true
                assertFalse(awaitItem().isVisible)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun lastSyncedSpanReflectsStoredTimestamp() =
        runTest(testDispatcher) {
            val repository =
                FakeConnectionStatusRepository(
                    initiallyConnected = true,
                    lastContact = Clock.System.now() - 2.hours,
                )
            val viewModel = ConnectivityBannerViewModel(repository)

            viewModel.state.test {
                repository.isConnected.value = false
                advanceTimeBy(ConnectivityBannerViewModel.GRACE_PERIOD + 1.seconds)

                val state = expectMostRecentItem()
                assertTrue(state.isVisible)
                // Bucket type only, not exact value: the span is computed against the real
                // wall clock (the ViewModel deliberately has no injectable clock).
                assertIs<RelativeTimeSpan.Hours>(state.lastSyncedSpan)
                cancelAndConsumeRemainingEvents()
            }
        }
}
