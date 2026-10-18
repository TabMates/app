package de.tabmates.features.tabgroup.presentation.navigation.groupschedules

import app.cash.turbine.test
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeRecurringSeriesRepository
import de.tabmates.features.tabgroup.presentation.testing.RecurringFixtures
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSchedulesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `splits the group's schedules into active and ended`() =
        runTest(dispatcher) {
            val recurring = FakeRecurringSeriesRepository()
            recurring.setSeries(
                RecurringFixtures.series(seriesId = "live", isActive = true),
                // Ended schedules stay listed: they explain entries that already exist, and hiding
                // them would make those entries look like they came from nowhere.
                RecurringFixtures.series(seriesId = "done", isActive = false),
            )

            viewModel(recurring).state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertEquals(listOf("live"), state.active.map { it.seriesId })
                assertEquals(listOf("done"), state.ended.map { it.seriesId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `leaves out schedules belonging to another group`() =
        runTest(dispatcher) {
            val recurring = FakeRecurringSeriesRepository()
            recurring.setSeries(
                RecurringFixtures.series(seriesId = "ours", groupId = "g1"),
                RecurringFixtures.series(seriesId = "theirs", groupId = "g2"),
            )

            viewModel(recurring).state.test {
                advanceUntilIdle()
                assertEquals(listOf("ours"), expectMostRecentItem().active.map { it.seriesId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a group with no schedules reports empty rather than loading`() =
        runTest(dispatcher) {
            viewModel(FakeRecurringSeriesRepository()).state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertTrue(state.isEmpty)
                assertTrue(!state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun viewModel(recurring: FakeRecurringSeriesRepository) =
        GroupSchedulesViewModel(
            groupId = "g1",
            recurringSeriesRepository = recurring,
            currencyRepository = FakeCurrencyRepository(),
        )
}
