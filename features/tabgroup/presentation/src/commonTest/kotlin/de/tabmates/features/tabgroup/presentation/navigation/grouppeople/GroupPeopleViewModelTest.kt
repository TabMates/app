package de.tabmates.features.tabgroup.presentation.navigation.grouppeople

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeCurrentAccount
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeScheduledLedger
import de.tabmates.features.tabgroup.presentation.testing.FakeTabEntryRepository
import de.tabmates.features.tabgroup.presentation.testing.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.StringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_remove_error_not_member
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_remove_error_stale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupPeopleViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val alice = Fixtures.participant(id = "user-1", name = "Alice")
    private val bob = Fixtures.participant(id = "user-2", name = "Bob")
    private val carol = Fixtures.participant(id = "user-3", name = "Carol")
    private val tom =
        Fixtures.participant(id = "ph-1", name = "Tom", type = ParticipantType.PLACEHOLDER)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * The state flow is `WhileSubscribed`, so it only leaves its initial value while something
     * collects it — the screen does that in production, and this stands in for it.
     */
    private fun TestScope.viewModel(
        repo: FakeGroupRepository,
        currentUserId: String? = "user-2",
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
    ): GroupPeopleViewModel {
        val viewModel =
            GroupPeopleViewModel(
                groupId = "g1",
                groupRepository = repo,
                scheduledLedger = FakeScheduledLedger(tabEntryRepository),
                currencyRepository = FakeCurrencyRepository(),
                exchangeRateRepository = FakeExchangeRateRepository(),
                currentAccount = FakeCurrentAccount(id = currentUserId),
                numberSymbols = NumberSymbols.Fallback,
            )
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun splitsMembersFromPlaceholdersAndBadgesThem() =
        runTest(testDispatcher) {
            val group =
                Fixtures.group(
                    id = "g1",
                    participants = setOf(alice, bob, tom),
                    creator = alice,
                    inviteToken = "tok",
                )
            val repo = FakeGroupRepository(initialGroups = listOf(group))

            val state = viewModel(repo).state.value

            assertFalse(state.isLoading)
            assertEquals(listOf("Bob", "Alice"), state.members.map { it.name })
            assertEquals(listOf("Tom"), state.placeholders.map { it.name })
            assertEquals(PersonBadge.PENDING, state.placeholders.single().badge)
            assertEquals("tok", state.inviteToken)
        }

    @Test
    fun currentUserSortsFirstAndKeepsTheOwnerBadge() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice, bob), creator = bob)
            val repo = FakeGroupRepository(initialGroups = listOf(group))

            // Bob is both the viewer and the creator: he reads as "You" and still keeps Owner.
            val members = viewModel(repo, currentUserId = "user-2").state.value.members

            assertEquals("Bob", members.first().name)
            assertTrue(members.first().isCurrentUser)
            assertEquals(PersonBadge.OWNER, members.first().badge)
            assertFalse(members[1].isCurrentUser)
            assertNull(members[1].badge)
        }

    @Test
    fun eachSubmittedNameIsAddedAndTheFieldStaysOpen() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice))
            val repo =
                FakeGroupRepository(initialGroups = listOf(group)).apply {
                    addNewParticipantsResult = Result.Success(group)
                }
            val viewModel = viewModel(repo)

            viewModel.onAction(GroupPeopleAction.AddPlaceholderClick)
            viewModel.submit("Tom")
            viewModel.submit("Max")

            assertEquals(
                listOf(listOf("Tom"), listOf("Max")),
                repo.addNewParticipantsCalls.map { it.usernames },
            )
            assertEquals("g1", repo.addNewParticipantsCalls.first().groupId)
            val state = viewModel.state.value
            // Still open and empty, so the next name can go straight in.
            assertTrue(state.isAddRowVisible)
            assertEquals("", state.newNameTextState.text.toString())
        }

    @Test
    fun submittingAnEmptyNameClosesTheRow() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice))
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = viewModel(repo)

            viewModel.onAction(GroupPeopleAction.AddPlaceholderClick)
            viewModel.submit("   ")

            assertTrue(repo.addNewParticipantsCalls.isEmpty())
            assertFalse(viewModel.state.value.isAddRowVisible)
        }

    @Test
    fun namesAlreadyInTheGroupAreRejectedWithAnError() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice, tom))
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = viewModel(repo)
            viewModel.onAction(GroupPeopleAction.AddPlaceholderClick)

            viewModel.events.test {
                // Already a placeholder, in a different case.
                viewModel.submit("tom")
                assertIs<GroupPeopleEvent.Error>(awaitItem())
                // Already a member.
                viewModel.submit("alice")
                assertIs<GroupPeopleEvent.Error>(awaitItem())
            }

            assertTrue(repo.addNewParticipantsCalls.isEmpty())
            assertTrue(viewModel.state.value.isAddRowVisible)
            // The rejected name stays in the field so it can be corrected.
            assertEquals(
                "alice",
                viewModel.state.value.newNameTextState.text
                    .toString(),
            )
        }

    @Test
    fun aMissingGroupResolvesInsteadOfLoadingForever() =
        runTest(testDispatcher) {
            val repo = FakeGroupRepository(initialGroups = emptyList())

            val state = viewModel(repo).state.value

            assertFalse(state.isLoading)
            assertTrue(state.members.isEmpty())
            assertTrue(state.placeholders.isEmpty())
        }

    @Test
    fun addFailureEmitsErrorAndKeepsTheTypedName() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice))
            val repo =
                FakeGroupRepository(initialGroups = listOf(group)).apply {
                    addNewParticipantsResult = Result.Failure(DataError.Remote.UNKNOWN)
                }
            val viewModel = viewModel(repo)

            viewModel.onAction(GroupPeopleAction.AddPlaceholderClick)
            viewModel.type("Tom")

            viewModel.events.test {
                viewModel.onAction(GroupPeopleAction.SubmitName)
                advanceUntilIdle()
                assertIs<GroupPeopleEvent.Error>(awaitItem())
            }
            val state = viewModel.state.value
            assertEquals("Tom", state.newNameTextState.text.toString())
            assertTrue(state.isAddRowVisible)
            assertFalse(state.isAddingPlaceholder)
        }

    @Test
    fun cancelClosesTheRowAndClearsTheField() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice))
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = viewModel(repo)

            viewModel.onAction(GroupPeopleAction.AddPlaceholderClick)
            viewModel.type("Tom")
            viewModel.onAction(GroupPeopleAction.CancelAdd)

            assertTrue(repo.addNewParticipantsCalls.isEmpty())
            assertFalse(viewModel.state.value.isAddRowVisible)
            assertEquals(
                "",
                viewModel.state.value.newNameTextState.text
                    .toString(),
            )
        }

    @Test
    fun rotateInviteDelegatesAndTheNewTokenFlowsBack() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice), inviteToken = "old")
            val repo =
                FakeGroupRepository(initialGroups = listOf(group)).apply {
                    rotateInviteResult = Result.Success(group.copy(inviteToken = "new"))
                }
            val viewModel = viewModel(repo)
            assertEquals("old", viewModel.state.value.inviteToken)

            viewModel.onAction(GroupPeopleAction.RotateInvite)
            advanceUntilIdle()
            assertEquals(listOf("g1"), repo.rotateInviteCalls)

            // The repository is the source of truth: the screen only updates once the rotated group
            // comes back through the groups flow.
            repo.emitGroups(listOf(group.copy(inviteToken = "new")))
            advanceUntilIdle()
            assertEquals("new", viewModel.state.value.inviteToken)
        }

    @Test
    fun rotateFailureEmitsError() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice), inviteToken = "old")
            val repo =
                FakeGroupRepository(initialGroups = listOf(group)).apply {
                    rotateInviteResult = Result.Failure(DataError.Remote.UNKNOWN)
                }
            val viewModel = viewModel(repo)

            viewModel.events.test {
                viewModel.onAction(GroupPeopleAction.RotateInvite)
                advanceUntilIdle()
                assertIs<GroupPeopleEvent.Error>(awaitItem())
            }
        }

    // region remove

    @Test
    fun onlyOtherPeopleCanBeRemoved() =
        runTest(testDispatcher) {
            val group =
                Fixtures.group(id = "g1", participants = setOf(alice, bob, carol, tom), creator = alice)
            val repo = FakeGroupRepository(initialGroups = listOf(group))

            // Bob is the viewer; Alice created the group.
            val state = viewModel(repo, currentUserId = "user-2").state.value

            assertEquals(
                mapOf("Bob" to false, "Alice" to false, "Carol" to true),
                state.members.associate { it.name to it.canRemove },
            )
            assertTrue(state.placeholders.single().canRemove)
        }

    @Test
    fun requestingAndDismissingLeavesNothingBehind() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice, bob), creator = bob)
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = viewModel(repo)

            viewModel.onAction(GroupPeopleAction.RemoveClick("user-1"))
            advanceUntilIdle()
            val target = assertNotNull(viewModel.state.value.removeTarget)
            assertEquals("Alice", target.name)
            assertFalse(target.isPlaceholder)
            // Nothing owed, so the dialog says nothing about money.
            assertNull(target.outstanding)

            viewModel.onAction(GroupPeopleAction.DismissRemove)
            assertNull(viewModel.state.value.removeTarget)
            assertTrue(repo.removeParticipantCalls.isEmpty())
        }

    @Test
    fun confirmIssuesOneCallForTheRequestedPerson() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice, bob), creator = bob)
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val viewModel = viewModel(repo)

            viewModel.onAction(GroupPeopleAction.RemoveClick("user-1"))
            advanceUntilIdle()
            viewModel.onAction(GroupPeopleAction.ConfirmRemove)
            // A second tap while the first is in flight must not issue a second removal.
            viewModel.onAction(GroupPeopleAction.ConfirmRemove)
            advanceUntilIdle()

            assertEquals(
                listOf(FakeGroupRepository.RemoveParticipantCall("g1", "user-1")),
                repo.removeParticipantCalls,
            )
            assertNull(viewModel.state.value.removeTarget)
            assertFalse(viewModel.state.value.isRemoving)
        }

    @Test
    fun anUnsettledBalanceIsWarnedAboutButNeverBlocks() =
        runTest(testDispatcher) {
            val group = Fixtures.group(id = "g1", participants = setOf(alice, bob), creator = bob)
            val repo = FakeGroupRepository(initialGroups = listOf(group))
            val entries =
                FakeTabEntryRepository().apply {
                    // Alice paid 100 and owes 50 of it, so she is owed 50.
                    emit(
                        "g1",
                        listOf(
                            Fixtures.expense(
                                id = "e1",
                                groupId = "g1",
                                amount = 100.0,
                                paidByUserId = "user-1",
                                splits =
                                    listOf(
                                        Fixtures.split(
                                            tabEntryId = "e1",
                                            participantId = "user-1",
                                            resolvedAmount = 50.0,
                                        ),
                                        Fixtures.split(
                                            tabEntryId = "e1",
                                            participantId = "user-2",
                                            resolvedAmount = 50.0,
                                        ),
                                    ),
                            ),
                        ),
                    )
                }
            val viewModel = viewModel(repo, tabEntryRepository = entries)

            viewModel.onAction(GroupPeopleAction.RemoveClick("user-1"))
            advanceUntilIdle()

            // Unsigned: the sentence says what is open, not who owes whom.
            assertEquals(
                "€50.00",
                viewModel.state.value.removeTarget
                    ?.outstanding,
            )
            viewModel.onAction(GroupPeopleAction.ConfirmRemove)
            advanceUntilIdle()
            assertEquals(1, repo.removeParticipantCalls.size)
        }

    @Test
    fun eachRefusalGetsItsOwnMessage() =
        runTest(testDispatcher) {
            val expected =
                mapOf(
                    // Told apart by their body code, so each carries its own global wording.
                    DataError.Remote.CANNOT_REMOVE_SELF to DataError.Remote.CANNOT_REMOVE_SELF.resource(),
                    DataError.Remote.CANNOT_REMOVE_GROUP_CREATOR to
                        DataError.Remote.CANNOT_REMOVE_GROUP_CREATOR.resource(),
                    // These two share a status with other meanings, so this screen overrides the
                    // generic copy — asserted below to differ from it.
                    DataError.Remote.FORBIDDEN to Res.string.group_people_remove_error_not_member,
                    DataError.Remote.NOT_FOUND to Res.string.group_people_remove_error_stale,
                )
            assertEquals(4, expected.values.distinct().size)
            expected.forEach { (error, resource) ->
                val group = Fixtures.group(id = "g1", participants = setOf(alice, bob), creator = bob)
                val repo =
                    FakeGroupRepository(initialGroups = listOf(group)).apply {
                        removeParticipantResult = Result.Failure(error)
                    }
                val viewModel = viewModel(repo)
                viewModel.onAction(GroupPeopleAction.RemoveClick("user-1"))
                advanceUntilIdle()

                viewModel.events.test {
                    viewModel.onAction(GroupPeopleAction.ConfirmRemove)
                    advanceUntilIdle()
                    val event = assertIs<GroupPeopleEvent.Error>(awaitItem())
                    assertEquals(resource, assertIs<UiText.Resource>(event.message).id)
                }
                // The dialog closes either way; the snackbar carries the reason.
                assertNull(viewModel.state.value.removeTarget)
            }
        }

    private fun DataError.Remote.resource(): StringResource = assertIs<UiText.Resource>(toUiText()).id

    // endregion

    private fun GroupPeopleViewModel.type(name: String) {
        state.value.newNameTextState.edit { replace(0, length, name) }
        Snapshot.sendApplyNotifications()
    }

    /** The unconfined dispatcher runs the add through to completion before this returns. */
    private fun GroupPeopleViewModel.submit(name: String) {
        type(name)
        onAction(GroupPeopleAction.SubmitName)
    }
}
