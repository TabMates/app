package de.tabmates.features.authentication.presentation.forgotpassword

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
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
    fun initialStateIsCorrect() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertFalse(state.canSubmit)
                assertFalse(state.isLoading)
                assertFalse(state.isEmailSentSuccessfully)
                assertNull(state.errorText)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun canSubmitIsTrueWhenEmailIsValid() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.emailTextFieldState.edit {
                replace(0, length, "test@example.com")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.state.test {
                assertTrue(expectMostRecentItem().canSubmit)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun canSubmitIsFalseWhenEmailIsInvalid() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.emailTextFieldState.edit {
                replace(0, length, "invalid-email")
            }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.state.test {
                assertFalse(expectMostRecentItem().canSubmit)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitForgotPasswordRequestSuccessSetsStateCorrectly() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(forgotPasswordResult = Result.Success(Unit))
            val viewModel = createViewModel(authService = authService)
            fillValidEmail(viewModel)

            viewModel.submitForgotPasswordRequest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertTrue(state.isEmailSentSuccessfully)
                assertFalse(state.isLoading)
                assertNull(state.errorText)
                assertEquals(1, authService.forgotPasswordCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitForgotPasswordRequestFailureSetsError() =
        runTest(testDispatcher) {
            val error = DataError.Remote.NOT_FOUND
            val authService = FakeAuthService(forgotPasswordResult = Result.Failure(error))
            val viewModel = createViewModel(authService = authService)
            fillValidEmail(viewModel)

            viewModel.submitForgotPasswordRequest()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertFalse(state.isEmailSentSuccessfully)
                assertFalse(state.isLoading)
                val errorText = assertIs<UiText.Resource>(state.errorText)
                val expectedErrorText = assertIs<UiText.Resource>(error.toUiText())
                assertEquals(expectedErrorText.id, errorText.id)
                assertEquals(1, authService.forgotPasswordCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun submitForgotPasswordRequestDoesNothingWhenCanSubmitIsFalse() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)

            viewModel.submitForgotPasswordRequest()
            advanceUntilIdle()

            assertEquals(0, authService.forgotPasswordCalls)
            assertFalse(viewModel.state.value.isLoading)
        }

    private fun TestScope.activateState(viewModel: ForgotPasswordViewModel) {
        backgroundScope.launch {
            viewModel.state.collect { }
        }
        advanceUntilIdle()
    }

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(),
    ): ForgotPasswordViewModel {
        val viewModel = ForgotPasswordViewModel(authService = authService)
        activateState(viewModel)
        return viewModel
    }

    private fun TestScope.fillValidEmail(viewModel: ForgotPasswordViewModel) {
        viewModel.state.value.emailTextFieldState.edit {
            replace(0, length, "test@example.com")
        }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }
}
