package de.tabmates.features.authentication.presentation.environment

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.environment.CustomEnvironment
import de.tabmates.core.domain.environment.EnvironmentRepository
import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.environment.EnvironmentSwitcher
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.testing.environment.FakeEnvironmentRepository
import de.tabmates.core.testing.environment.customEnvironmentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_key_rejected
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_unreachable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentViewModelTest {
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
    fun applyStaysDisabledUntilBothFieldsAreFilled() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.test {
                assertFalse(awaitItem().canApply)
                cancelAndConsumeRemainingEvents()
            }

            fillIn(viewModel, url = "https://custom.example.com", apiKey = "")

            viewModel.state.test {
                assertFalse(expectMostRecentItem().canApply)
                cancelAndConsumeRemainingEvents()
            }

            fillIn(viewModel, url = "https://custom.example.com", apiKey = "custom-key")

            viewModel.state.test {
                assertTrue(expectMostRecentItem().canApply)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun applyingSwitchesAndClosesTheScreen() =
        runTest(testDispatcher) {
            val switcher = FakeEnvironmentSwitcher()
            val viewModel = createViewModel(switcher = switcher)
            fillIn(viewModel, url = " https://custom.example.com ", apiKey = " custom-key ")

            viewModel.events.test {
                viewModel.onApplyCustom()

                assertIs<EnvironmentEvent.Switched>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
            // Trimmed on the way out: a copy-pasted URL usually brings whitespace with it.
            assertEquals(listOf("https://custom.example.com" to "custom-key"), switcher.customCalls)
        }

    @Test
    fun aRejectedSwitchSurfacesTheReasonAndEmitsNothing() =
        runTest(testDispatcher) {
            val switcher =
                FakeEnvironmentSwitcher(result = Result.Failure(EnvironmentSwitchError.KEY_REJECTED))
            val viewModel = createViewModel(switcher = switcher)
            fillIn(viewModel, url = "https://custom.example.com", apiKey = "wrong-key")

            viewModel.events.test {
                viewModel.onApplyCustom()

                expectNoEvents()
                cancelAndConsumeRemainingEvents()
            }

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertFalse(state.isApplying)
                // Under the api-key field, not the URL one: that is the input the user has to fix.
                assertEquals(Res.string.environment_error_key_rejected, (state.apiKeyError as UiText.Resource).id)
                assertNull(state.urlError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun anUnreachableHostIsBlamedOnTheUrlField() =
        runTest(testDispatcher) {
            val switcher =
                FakeEnvironmentSwitcher(result = Result.Failure(EnvironmentSwitchError.UNREACHABLE))
            val viewModel = createViewModel(switcher = switcher)
            fillIn(viewModel, url = "https://nope.example.com", apiKey = "custom-key")

            viewModel.onApplyCustom()
            advanceUntilIdle()

            viewModel.state.test {
                val state = expectMostRecentItem()
                assertEquals(Res.string.environment_error_unreachable, (state.urlError as UiText.Resource).id)
                assertNull(state.apiKeyError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun goingBackToTheDefaultIsOnlyPossibleWhileACustomOneIsActive() =
        runTest(testDispatcher) {
            val switcher = FakeEnvironmentSwitcher()
            val viewModel = createViewModel(switcher = switcher)

            viewModel.onUseDefault()
            advanceUntilIdle()

            assertEquals(0, switcher.defaultCalls)
        }

    @Test
    fun goingBackToTheDefaultSwitchesAndClosesTheScreen() =
        runTest(testDispatcher) {
            val switcher = FakeEnvironmentSwitcher()
            val viewModel =
                createViewModel(
                    switcher = switcher,
                    repository = FakeEnvironmentRepository(initial = customEnvironmentConfig()),
                )

            viewModel.events.test {
                viewModel.onUseDefault()

                assertIs<EnvironmentEvent.Switched>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
            assertEquals(1, switcher.defaultCalls)
        }

    @Test
    fun theStoredCustomEnvironmentIsPrefilled() =
        runTest(testDispatcher) {
            val repository =
                FakeEnvironmentRepository(
                    storedCustom = CustomEnvironment("https://stored.example.com", "stored-key"),
                )
            val viewModel = createViewModel(repository = repository)

            viewModel.state.test {
                val state = awaitItem()
                assertEquals("https://stored.example.com", state.urlTextFieldState.text.toString())
                assertEquals("stored-key", state.apiKeyTextFieldState.text.toString())
                assertNull(state.urlError)
                assertNull(state.apiKeyError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun thePickerOpensOnTheEnvironmentThatIsLive() =
        runTest(testDispatcher) {
            assertEquals(EnvironmentMode.DEFAULT, stateOf(createViewModel()).selectedMode)

            val onCustom =
                createViewModel(repository = FakeEnvironmentRepository(initial = customEnvironmentConfig()))

            assertEquals(EnvironmentMode.CUSTOM, stateOf(onCustom).selectedMode)
        }

    @Test
    fun switchingRowsClearsAPendingError() =
        runTest(testDispatcher) {
            val switcher =
                FakeEnvironmentSwitcher(result = Result.Failure(EnvironmentSwitchError.UNREACHABLE))
            val viewModel = createViewModel(switcher = switcher)
            viewModel.onModeSelected(EnvironmentMode.CUSTOM)
            fillIn(viewModel, url = "https://nope.example.com", apiKey = "custom-key")
            viewModel.onApplyCustom()
            advanceUntilIdle()

            viewModel.onModeSelected(EnvironmentMode.DEFAULT)

            val state = stateOf(viewModel)
            assertNull(state.urlError)
            assertNull(state.apiKeyError)
        }

    @Test
    fun theDefaultRowOnlySubmitsWhileACustomEnvironmentIsActive() =
        runTest(testDispatcher) {
            assertFalse(stateOf(createViewModel()).canSubmit)

            val onCustom =
                createViewModel(repository = FakeEnvironmentRepository(initial = customEnvironmentConfig()))
            onCustom.onModeSelected(EnvironmentMode.DEFAULT)

            assertTrue(stateOf(onCustom).canSubmit)
        }

    @Test
    fun theCustomRowOnlySubmitsOnceBothFieldsAreFilled() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onModeSelected(EnvironmentMode.CUSTOM)

            assertFalse(stateOf(viewModel).canSubmit)

            fillIn(viewModel, url = "https://custom.example.com", apiKey = "custom-key")

            assertTrue(stateOf(viewModel).canSubmit)
        }

    @Test
    fun theApiKeyStartsMaskedAndTheToggleRevealsIt() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            assertFalse(stateOf(viewModel).isApiKeyVisible)

            viewModel.onToggleApiKeyVisibility()

            assertTrue(stateOf(viewModel).isApiKeyVisible)
        }

    @Test
    fun submitFollowsThePickedRow() =
        runTest(testDispatcher) {
            val switcher = FakeEnvironmentSwitcher()
            val viewModel =
                createViewModel(
                    switcher = switcher,
                    repository = FakeEnvironmentRepository(initial = customEnvironmentConfig()),
                )

            viewModel.onModeSelected(EnvironmentMode.CUSTOM)
            fillIn(viewModel, url = "https://custom.example.com", apiKey = "custom-key")
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(listOf("https://custom.example.com" to "custom-key"), switcher.customCalls)
            assertEquals(0, switcher.defaultCalls)

            viewModel.onModeSelected(EnvironmentMode.DEFAULT)
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(1, switcher.defaultCalls)
        }

    private fun createViewModel(
        switcher: EnvironmentSwitcher = FakeEnvironmentSwitcher(),
        repository: EnvironmentRepository = FakeEnvironmentRepository(),
    ) = EnvironmentViewModel(environmentSwitcher = switcher, environmentRepository = repository)

    /**
     * Resubscribes to read the state: the flow stops mirroring `_state` once the screen — here, the
     * test — has been away longer than `WhileSubscribed`'s timeout, so a plain `.value` goes stale.
     */
    private suspend fun stateOf(viewModel: EnvironmentViewModel): EnvironmentState {
        lateinit var state: EnvironmentState
        viewModel.state.test {
            state = expectMostRecentItem()
            cancelAndConsumeRemainingEvents()
        }
        return state
    }

    private suspend fun fillIn(
        viewModel: EnvironmentViewModel,
        url: String,
        apiKey: String,
    ) {
        // Edited while collecting: the input observers live in the state flow's `onStart`, so
        // without a collector `canApply` never updates — in the app the screen is the collector.
        viewModel.state.test {
            awaitItem()
            viewModel.state.value.urlTextFieldState
                .edit { replace(0, length, url) }
            viewModel.state.value.apiKeyTextFieldState
                .edit { replace(0, length, apiKey) }
            Snapshot.sendApplyNotifications()
            cancelAndConsumeRemainingEvents()
        }
    }

    private class FakeEnvironmentSwitcher(
        private val result: EmptyResult<EnvironmentSwitchError> = Result.Success(Unit),
    ) : EnvironmentSwitcher {
        val customCalls: MutableList<Pair<String, String>> = mutableListOf()
        var defaultCalls: Int = 0
            private set

        override suspend fun useCustom(
            httpBaseUrl: String,
            apiKey: String,
        ): EmptyResult<EnvironmentSwitchError> {
            customCalls += httpBaseUrl to apiKey
            return result
        }

        override suspend fun useDefault() {
            defaultCalls += 1
        }
    }
}
