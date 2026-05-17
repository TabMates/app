package de.tabmates.features.tabgroup.presentation.navigation.groupsettings

import app.cash.turbine.test
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.Fixtures
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSettingsViewModelTest {
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
    fun loadHydratesFieldsFromGroup() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", title = "Lisbon", currency = "EUR")
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = GroupSettingsViewModel(groupId = "g1", groupRepository = repo)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("Lisbon", state.nameTextState.text.toString())
            assertEquals("EUR", state.defaultCurrencyCode)
        }

    @Test
    fun saveCallsUpdateAndEmitsSaved() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", title = "Lisbon", currency = "EUR")
            val repo =
                FakeGroupRepository(initialGroups = listOf(group)).apply {
                    updateGroupResult = Result.Success(group.copy(title = "Lisbon Updated"))
                }
            val viewModel = GroupSettingsViewModel(groupId = "g1", groupRepository = repo)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onAction(GroupSettingsAction.Save)
                advanceUntilIdle()
                assertIs<GroupSettingsEvent.Saved>(awaitItem())
            }
            assertEquals(1, repo.updateGroupCalls.size)
            assertEquals("g1", repo.updateGroupCalls.single().groupId)
            assertEquals("Lisbon", repo.updateGroupCalls.single().title)
        }

    @Test
    fun saveValidationErrorEmitsErrorWithoutCallingRepo() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", title = "", currency = "EUR")
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = GroupSettingsViewModel(groupId = "g1", groupRepository = repo)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onAction(GroupSettingsAction.Save)
                advanceUntilIdle()
                assertIs<GroupSettingsEvent.Error>(awaitItem())
            }
            assertTrue(repo.updateGroupCalls.isEmpty())
        }

    @Test
    fun requestLeaveShowsDialog() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1")
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = GroupSettingsViewModel(groupId = "g1", groupRepository = repo)
            advanceUntilIdle()

            viewModel.onAction(GroupSettingsAction.RequestLeave)
            assertTrue(viewModel.state.value.showLeaveDialog)

            viewModel.onAction(GroupSettingsAction.DismissLeaveDialog)
            assertFalse(viewModel.state.value.showLeaveDialog)
        }

    @Test
    fun confirmLeaveEmitsLeft() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1")
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = GroupSettingsViewModel(groupId = "g1", groupRepository = repo)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onAction(GroupSettingsAction.ConfirmLeave)
                advanceUntilIdle()
                assertIs<GroupSettingsEvent.Left>(awaitItem())
            }
        }
}
