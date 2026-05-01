package de.tabmates.features.authentication.presentation.registerguest

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.util.UiText
import de.tabmates.features.authentication.testing.FakeAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterGuestViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dummyAuthInfo =
        AuthInfo(
            accessToken = "access",
            refreshToken = "refresh",
            user =
                User(
                    id = "1",
                    email = "guest@example.com",
                    username = "Guest",
                    hasVerifiedEmail = false,
                    userType = UserType.ANONYMOUS,
                ),
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsCorrect() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertEquals("", state.usernameTextState.text.toString())
                assertFalse(state.isRegistering)
                assertNull(state.usernameError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun registerGuestWithTooShortUsernameSetsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.usernameTextState.edit {
                replace(0, length, "ab")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.registerGuest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertIs<UiText.Resource>(state.usernameError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun registerGuestWithTooLongUsernameSetsError() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.usernameTextState.edit {
                replace(0, length, "a".repeat(21))
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.registerGuest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertIs<UiText.Resource>(state.usernameError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun registerGuestSuccessEmitsSuccessEvent() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    registerAnonymousResult = Result.Success(dummyAuthInfo),
                )
            val viewModel = createViewModel(authService = authService)
            fillValidUsername(viewModel)

            viewModel.events.test {
                viewModel.registerGuest()
                assertEquals(RegisterGuestEvent.Success, awaitItem())
                assertEquals(1, authService.registerAnonymousCalls)
                cancelAndConsumeRemainingEvents()
            }

            viewModel.state.test {
                assertFalse(expectMostRecentItem().isRegistering)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun registerGuestFailureEmitsErrorEvent() =
        runTest(testDispatcher) {
            val error = DataError.Remote.CONFLICT
            val authService =
                FakeAuthService(
                    registerAnonymousResult = Result.Failure(error),
                )
            val viewModel = createViewModel(authService = authService)
            fillValidUsername(viewModel)

            viewModel.events.test {
                viewModel.registerGuest()
                val event = assertIs<RegisterGuestEvent.RegistrationError>(awaitItem())
                assertIs<UiText.Resource>(event.message)
                assertEquals(1, authService.registerAnonymousCalls)
                cancelAndConsumeRemainingEvents()
            }

            viewModel.state.test {
                assertFalse(expectMostRecentItem().isRegistering)
                cancelAndConsumeRemainingEvents()
            }
        }

    private fun TestScope.activateState(viewModel: RegisterGuestViewModel) {
        backgroundScope.launch {
            viewModel.state.collect { }
        }
        advanceUntilIdle()
    }

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(),
    ): RegisterGuestViewModel {
        val viewModel = RegisterGuestViewModel(authService = authService)
        activateState(viewModel)
        return viewModel
    }

    private fun TestScope.fillValidUsername(viewModel: RegisterGuestViewModel) {
        viewModel.state.value.usernameTextState.edit {
            replace(0, length, "ValidUsername")
        }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }
}
