package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.authentication.testing.FakeAuthService
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MigrateAccountViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(authService: FakeAuthService) = MigrateAccountViewModel(authService)

    private fun MigrateAccountViewModel.fillValid(
        email: String = "user@example.com",
        password: String = "secret12",
        confirm: String = "secret12",
    ) {
        emailState.setTextAndPlaceCursorAtEnd(email)
        passwordState.setTextAndPlaceCursorAtEnd(password)
        confirmPasswordState.setTextAndPlaceCursorAtEnd(confirm)
    }

    @Test
    fun validInputMigratesAndEmitsMigratedWithEmail() =
        runTest(testDispatcher) {
            val auth = FakeAuthService(migrateToRegisteredResult = Result.Success(Unit))
            val vm = viewModel(auth)
            vm.fillValid(email = "user@example.com", password = "secret12", confirm = "secret12")

            vm.events.test {
                vm.onSave()
                advanceUntilIdle()

                val event = assertIs<MigrateAccountEvent.Migrated>(awaitItem())
                assertEquals("user@example.com", event.email)
            }
            assertEquals("user@example.com" to "secret12", auth.migrateToRegisteredCalls.single())
            assertEquals(false, vm.state.value.isSubmitting)
        }

    @Test
    fun invalidEmailSetsErrorAndDoesNotCallService() =
        runTest(testDispatcher) {
            val auth = FakeAuthService()
            val vm = viewModel(auth)
            vm.fillValid(email = "not-an-email")

            vm.onSave()
            advanceUntilIdle()

            assertNotNull(vm.state.value.emailError)
            assertTrue(auth.migrateToRegisteredCalls.isEmpty())
        }

    @Test
    fun weakPasswordSetsErrorAndDoesNotCallService() =
        runTest(testDispatcher) {
            val auth = FakeAuthService()
            val vm = viewModel(auth)
            vm.fillValid(password = "short", confirm = "short")

            vm.onSave()
            advanceUntilIdle()

            assertNotNull(vm.state.value.passwordError)
            assertTrue(auth.migrateToRegisteredCalls.isEmpty())
        }

    @Test
    fun mismatchedConfirmSetsErrorAndDoesNotCallService() =
        runTest(testDispatcher) {
            val auth = FakeAuthService()
            val vm = viewModel(auth)
            vm.fillValid(password = "secret12", confirm = "secret34")

            vm.onSave()
            advanceUntilIdle()

            assertNotNull(vm.state.value.confirmPasswordError)
            assertTrue(auth.migrateToRegisteredCalls.isEmpty())
        }

    @Test
    fun conflictEmitsErrorEvent() =
        runTest(testDispatcher) {
            val auth = FakeAuthService(migrateToRegisteredResult = Result.Failure(DataError.Remote.CONFLICT))
            val vm = viewModel(auth)
            vm.fillValid()

            vm.events.test {
                vm.onSave()
                advanceUntilIdle()

                assertIs<MigrateAccountEvent.Error>(awaitItem())
            }
            assertEquals(false, vm.state.value.isSubmitting)
        }

    @Test
    fun validInputClearsPreviousFieldErrors() =
        runTest(testDispatcher) {
            val auth = FakeAuthService(migrateToRegisteredResult = Result.Success(Unit))
            val vm = viewModel(auth)

            // First submit with bad email to set an error.
            vm.fillValid(email = "bad")
            vm.onSave()
            advanceUntilIdle()
            assertNotNull(vm.state.value.emailError)

            // Fix and resubmit.
            vm.fillValid(email = "user@example.com")
            vm.events.test {
                vm.onSave()
                advanceUntilIdle()
                assertIs<MigrateAccountEvent.Migrated>(awaitItem())
            }
            assertNull(vm.state.value.emailError)
        }
}
