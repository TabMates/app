package de.tabmates.features.tabgroup.presentation.navigation.activity

import de.tabmates.features.tabgroup.domain.activity.ActivityEntryType
import de.tabmates.features.tabgroup.domain.activity.ActivityEvent
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityField
import de.tabmates.features.tabgroup.domain.activity.ActivityFieldChange
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeActivityRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeSessionStorage
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Clock.System.now()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun eventsAreBucketedByDay() =
        runTest(testDispatcher) {
            val activityRepo =
                FakeActivityRepository(
                    listOf(
                        persisted(id = "a2", seq = 2, occurredAt = now),
                        persisted(id = "a1", seq = 1, occurredAt = now - 1.days),
                    ),
                )
            val viewModel = createViewModel(activityRepo)
            activateState(viewModel)

            val sections = viewModel.state.value.sections
            assertEquals(listOf(ActivityBucket.Today, ActivityBucket.Yesterday), sections.map { it.bucket })
            assertEquals(listOf("a2"), sections[0].items.map { it.id })
            assertEquals(listOf("a1"), sections[1].items.map { it.id })
        }

    @Test
    fun pendingWritesRenderFirstAndCarryThePendingFlag() =
        runTest(testDispatcher) {
            val activityRepo =
                FakeActivityRepository(
                    listOf(
                        ActivityFeedItem.Pending(
                            outboxId = "e9",
                            tabEntryId = "e9",
                            type = ActivityEventType.ENTRY_CREATED,
                            groupId = "g1",
                            occurredAt = now,
                            entryType = ActivityEntryType.EXPENSE,
                            entryTitle = "Dinner",
                            amount = 12.5,
                            currencyCode = "EUR",
                        ),
                        persisted(id = "a1", seq = 1, occurredAt = now),
                    ),
                )
            val viewModel = createViewModel(activityRepo)
            activateState(viewModel)

            val items =
                viewModel.state.value.sections
                    .single()
                    .items
            assertEquals(listOf("pending-e9", "a1"), items.map { it.id })
            assertTrue(items.first().isPending)
            assertTrue(items.first().actorIsYou)
        }

    @Test
    fun loadMoreAsksTheRepositoryForALargerWindow() =
        runTest(testDispatcher) {
            val activityRepo = FakeActivityRepository()
            val viewModel = createViewModel(activityRepo)
            activateState(viewModel)
            val firstLimit = activityRepo.requestedLimits.first()

            viewModel.loadMore()
            advanceUntilIdle()

            assertTrue(activityRepo.requestedLimits.last() > firstLimit)
        }

    @Test
    fun entryEventsOpenTheEntryAndSettlementsTheSettlementRoute() =
        runTest(testDispatcher) {
            val activityRepo =
                FakeActivityRepository(
                    listOf(
                        persisted(id = "a1", seq = 1, occurredAt = now, entryType = ActivityEntryType.EXPENSE),
                        persisted(
                            id = "a2",
                            seq = 2,
                            occurredAt = now,
                            entryType = ActivityEntryType.SETTLEMENT,
                        ),
                    ),
                )
            val viewModel = createViewModel(activityRepo)
            activateState(viewModel)

            val targets =
                viewModel.state.value.sections
                    .single()
                    .items
                    .associate { it.id to it.clickTarget }
            val expense = assertIs<ActivityClickTarget.Entry>(targets["a1"])
            assertEquals("e1", expense.tabEntryId)
            assertEquals("g1", expense.groupId)
            assertEquals(false, expense.isSettlement)
            assertTrue(assertIs<ActivityClickTarget.Entry>(targets["a2"]).isSettlement)
        }

    @Test
    fun deletedEntriesAreStruckThroughAndNotClickable() =
        runTest(testDispatcher) {
            val activityRepo =
                FakeActivityRepository(
                    listOf(
                        persisted(
                            id = "a1",
                            seq = 1,
                            occurredAt = now,
                            type = ActivityEventType.ENTRY_DELETED,
                        ),
                    ),
                )
            val viewModel = createViewModel(activityRepo)
            activateState(viewModel)

            val item =
                viewModel.state.value.sections
                    .single()
                    .items
                    .single()
            assertTrue(item.isDeleted)
            assertEquals(ActivityClickTarget.None, item.clickTarget)
        }

    @Test
    fun memberEventsPointAtTheGroup() =
        runTest(testDispatcher) {
            val activityRepo =
                FakeActivityRepository(
                    listOf(
                        persisted(
                            id = "a1",
                            seq = 1,
                            occurredAt = now,
                            type = ActivityEventType.MEMBER_JOINED,
                            tabEntryId = null,
                        ),
                    ),
                )
            val viewModel = createViewModel(activityRepo)
            activateState(viewModel)

            val item =
                viewModel.state.value.sections
                    .single()
                    .items
                    .single()
            assertEquals(ActivityClickTarget.Group("g1"), item.clickTarget)
            assertIs<ActivityKind.MemberJoined>(item.kind)
        }

    @Test
    fun paidByDiffsResolveUserIdsToNames() =
        runTest(testDispatcher) {
            val activityRepo =
                FakeActivityRepository(
                    listOf(
                        persisted(
                            id = "a1",
                            seq = 1,
                            occurredAt = now,
                            type = ActivityEventType.ENTRY_UPDATED,
                            changes =
                                listOf(
                                    ActivityFieldChange(
                                        field = ActivityField.PAID_BY,
                                        oldValue = "user-1",
                                        newValue = "user-2",
                                    ),
                                ),
                        ),
                    ),
                )
            val viewModel = createViewModel(activityRepo)
            activateState(viewModel)

            val diff =
                viewModel.state.value.sections
                    .single()
                    .items
                    .single()
                    .diffs
                    .single()
            assertEquals("Alice", diff.oldValue)
            assertEquals("Bob", diff.newValue)
        }

    private fun persisted(
        id: String,
        seq: Long,
        occurredAt: Instant,
        type: ActivityEventType = ActivityEventType.ENTRY_CREATED,
        tabEntryId: String? = "e1",
        entryType: ActivityEntryType? = ActivityEntryType.EXPENSE,
        changes: List<ActivityFieldChange> = emptyList(),
    ): ActivityFeedItem.Persisted =
        ActivityFeedItem.Persisted(
            ActivityEvent(
                id = id,
                seq = seq,
                groupId = "g1",
                occurredAt = occurredAt,
                actorUserId = "user-1",
                type = type,
                tabEntryId = tabEntryId,
                entryType = entryType,
                entryTitle = "Dinner",
                amount = 12.5,
                currencyCode = "EUR",
                targetUserId = "user-2",
                targetUsername = "Bob",
                entryVersion = 0,
                changes = changes,
            ),
        )

    private fun TestScope.activateState(viewModel: ActivityViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(activityRepository: FakeActivityRepository): ActivityViewModel =
        ActivityViewModel(
            activityRepository = activityRepository,
            groupRepository =
                FakeGroupRepository(
                    initialGroups = listOf(Fixtures.group(id = "g1", title = "Trip")),
                    initialAllParticipants =
                        listOf(
                            Fixtures.participant(id = "user-1", name = "Alice"),
                            Fixtures.participant(id = "user-2", name = "Bob"),
                        ),
                ),
            currencyRepository = FakeCurrencyRepository(),
            sessionStorage = FakeSessionStorage(),
        )
}
