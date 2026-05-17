package de.tabmates.features.tabgroup.presentation.navigation.joingroup

import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
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
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class JoinGroupViewModelTest {
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
    fun previewSuccessExposesReadyStateWithPreview() =
        runTest(testDispatcher) {
            val preview = samplePreview()
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(preview)
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            val state = assertIs<JoinGroupState.Ready>(viewModel.state.value)
            assertEquals(preview, state.preview)
            assertEquals("tok", state.token)
            assertIs<JoinOption.NewMember>(state.selectedOption)
            assertEquals(listOf("tok"), repo.previewInviteCalls)
        }

    @Test
    fun previewNotFoundExposesExpiredState() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Failure(DataError.Remote.NOT_FOUND)
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            assertIs<JoinGroupState.Expired>(viewModel.state.value)
        }

    @Test
    fun previewOtherErrorFallsBackToReadyWithoutPreview() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Failure(DataError.Remote.UNKNOWN)
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            val state = assertIs<JoinGroupState.Ready>(viewModel.state.value)
            assertNull(state.preview)
        }

    @Test
    fun selectClaimUpdatesSelectedOption() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(samplePreview())
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            val claim = JoinOption.ClaimPlaceholder(placeholderId = "p1", placeholderName = "Dan")
            viewModel.onAction(JoinGroupAction.SelectOption(claim))
            advanceUntilIdle()

            val state = assertIs<JoinGroupState.Ready>(viewModel.state.value)
            assertEquals(claim, state.selectedOption)
        }

    @Test
    fun submitWithNewMemberPassesNullClaimId() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(samplePreview())
                    joinGroupResult = Result.Success(Fixtures.group(id = "g99"))
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onAction(JoinGroupAction.Submit)
                advanceUntilIdle()

                val event = awaitItem()
                assertIs<JoinGroupEvent.Joined>(event)
                assertEquals("g99", event.groupId)
            }
            assertEquals(1, repo.joinGroupCalls.size)
            assertEquals("tok", repo.joinGroupCalls.single().token)
            assertNull(repo.joinGroupCalls.single().claimPlaceholderId)
        }

    @Test
    fun submitWithClaimPassesPlaceholderId() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(samplePreview())
                    joinGroupResult = Result.Success(Fixtures.group(id = "g99"))
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            viewModel.onAction(
                JoinGroupAction.SelectOption(JoinOption.ClaimPlaceholder("p1", "Dan")),
            )

            viewModel.events.test {
                viewModel.onAction(JoinGroupAction.Submit)
                advanceUntilIdle()

                assertIs<JoinGroupEvent.Joined>(awaitItem())
            }
            assertEquals("p1", repo.joinGroupCalls.single().claimPlaceholderId)
        }

    @Test
    fun submitNotFoundRoutesToExpired() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(samplePreview())
                    joinGroupResult = Result.Failure(DataError.Remote.NOT_FOUND)
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            viewModel.onAction(JoinGroupAction.Submit)
            advanceUntilIdle()

            assertIs<JoinGroupState.Expired>(viewModel.state.value)
        }

    @Test
    fun submitBadRequestRoutesToExpired() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(samplePreview())
                    joinGroupResult = Result.Failure(DataError.Remote.BAD_REQUEST)
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            viewModel.onAction(JoinGroupAction.Submit)
            advanceUntilIdle()

            assertIs<JoinGroupState.Expired>(viewModel.state.value)
        }

    @Test
    fun submitOtherErrorEmitsErrorEventAndRestoresReady() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(samplePreview())
                    joinGroupResult = Result.Failure(DataError.Remote.UNKNOWN)
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onAction(JoinGroupAction.Submit)
                advanceUntilIdle()

                assertIs<JoinGroupEvent.Error>(awaitItem())
            }
            val state = assertIs<JoinGroupState.Ready>(viewModel.state.value)
            assertEquals(false, state.isJoining)
        }

    @Test
    fun secondSubmitWhileJoiningIsIgnored() =
        runTest(testDispatcher) {
            val repo =
                FakeGroupRepository().apply {
                    previewInviteResult = Result.Success(samplePreview())
                    joinGroupResult = Result.Success(Fixtures.group(id = "g99"))
                }
            val viewModel = JoinGroupViewModel(token = "tok", groupRepository = repo)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onAction(JoinGroupAction.Submit)
                viewModel.onAction(JoinGroupAction.Submit)
                advanceUntilIdle()

                assertIs<JoinGroupEvent.Joined>(awaitItem())
            }
            assertEquals(1, repo.joinGroupCalls.size)
        }

    private fun samplePreview(): GroupInvitePreview =
        GroupInvitePreview(
            title = "Weekend in Lisbon",
            inviterUsername = "Ben",
            memberCount = 4,
            placeholders =
                listOf(
                    GroupParticipant(
                        userId = "p1",
                        username = "Dan",
                        participantType = ParticipantType.PLACEHOLDER,
                    ),
                ),
        )
}
