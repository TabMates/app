package de.tabmates.features.authentication.presentation.registersuccess

import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.util.UiText
import de.tabmates.features.authentication.testing.FakeAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterSuccessViewModelTest {
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
    fun `initial state exposes registered email and default values`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(email = "test@example.com")

            assertEquals("test@example.com", viewModel.state.value.registeredEmail)
            assertEquals(false, viewModel.state.value.isResendingVerificationEmail)
            assertNull(viewModel.state.value.resendVerificationError)
        }

    @Test
    fun `resend verification success emits success event and resets loading state`() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = createViewModel(authService = authService)

            viewModel.events.test {
                viewModel.resendVerification()

                assertEquals(RegisterSuccessEvent.ResendVerificationEmailSuccess, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, authService.resendVerificationEmailCalls)
            assertEquals(false, viewModel.state.value.isResendingVerificationEmail)
            assertNull(viewModel.state.value.resendVerificationError)
        }

    @Test
    fun `resend verification failure sets error state and emits error event`() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    resendVerificationEmailResult = Result.Failure(DataError.Remote.SERVER_ERROR),
                )
            val viewModel = createViewModel(authService = authService)

            viewModel.events.test {
                viewModel.resendVerification()

                assertEquals(RegisterSuccessEvent.ResendVerificationEmailError, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, authService.resendVerificationEmailCalls)
            assertEquals(false, viewModel.state.value.isResendingVerificationEmail)
            assertIs<UiText.Resource>(viewModel.state.value.resendVerificationError)
        }

    @Test
    fun `resend verification ignores duplicate calls while request is in progress`() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    resendVerificationEmailDelayMillis = 1_000L,
                )
            val viewModel = createViewModel(authService = authService)

            viewModel.resendVerification()
            viewModel.resendVerification()

            assertEquals(1, authService.resendVerificationEmailCalls)

            advanceUntilIdle()
            assertEquals(false, viewModel.state.value.isResendingVerificationEmail)
        }

    private fun TestScope.activateState(viewModel: RegisterSuccessViewModel) {
        backgroundScope.launch {
            viewModel.state.collect()
        }
        advanceUntilIdle()
    }

    private fun TestScope.createViewModel(
        authService: FakeAuthService = FakeAuthService(),
        email: String = "user@tabmates.com",
    ): RegisterSuccessViewModel {
        val viewModel = RegisterSuccessViewModel(authService = authService, email = email)
        activateState(viewModel)
        return viewModel
    }
}
