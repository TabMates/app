package de.tabmates.features.tabgroup.presentation.navigation.home

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.testing.FakeAuthService
import de.tabmates.features.tabgroup.presentation.testing.FakeSessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onLogoutClickPassesRefreshTokenAndClearsSession() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val sessionStorage = FakeSessionStorage()
            val viewModel = HomeViewModel(authService, sessionStorage)

            viewModel.onLogoutClick()
            advanceUntilIdle()

            assertEquals(listOf("refresh"), authService.logoutCalls)
            assertNull(sessionStorage.get())
        }

    @Test
    fun onLogoutClickWithNoSessionPassesEmptyToken() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val sessionStorage = FakeSessionStorage(initial = null)
            val viewModel = HomeViewModel(authService, sessionStorage)

            viewModel.onLogoutClick()
            advanceUntilIdle()

            assertEquals(listOf(""), authService.logoutCalls)
        }

    @Test
    fun onLogoutClickFailureKeepsSession() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(logoutResult = Result.Failure(DataError.Remote.UNKNOWN))
            val sessionStorage = FakeSessionStorage()
            val viewModel = HomeViewModel(authService, sessionStorage)

            viewModel.onLogoutClick()
            advanceUntilIdle()

            assertNotNull(sessionStorage.get())
        }
}
