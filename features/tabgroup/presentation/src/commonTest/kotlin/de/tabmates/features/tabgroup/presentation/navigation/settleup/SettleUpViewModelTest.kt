package de.tabmates.features.tabgroup.presentation.navigation.settleup

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeCurrentAccount
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeTabEntryRepository
import de.tabmates.features.tabgroup.presentation.testing.Fixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettleUpViewModelTest {
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
    fun planIncludesDebtsBetweenOtherMembers() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(tabEntryRepository = repoWithSharedExpense())
            activateState(viewModel)
            advanceUntilIdle()

            val payments = viewModel.state.value.payments
            assertEquals(2, payments.size)
            val yours = payments.first { it.fromUserId == CURRENT_USER }
            assertEquals(PAYEE, yours.toUserId)
            assertEquals(10.0, yours.amount)
            val other = payments.first { it.fromUserId == OTHER_DEBTOR }
            assertEquals(PAYEE, other.toUserId)
            assertEquals(10.0, other.amount)
            assertEquals("Cara", other.fromName)
            assertEquals("Bob", other.toName)
        }

    @Test
    fun rowClickOpensDialogWithPrefilledAmount() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(tabEntryRepository = repoWithSharedExpense())
            activateState(viewModel)
            advanceUntilIdle()

            val payment =
                viewModel.state.value.payments
                    .first { it.fromUserId == OTHER_DEBTOR }
            viewModel.onPaymentRowClick(payment)

            assertEquals(payment, viewModel.pendingSettlement.value)
            assertEquals("10.00", viewModel.settleAmountTextState.text.toString())
        }

    @Test
    fun confirmRecordsSettlementOnBehalfOfOtherMember() =
        runTest(testDispatcher) {
            val tabEntryRepo = repoWithSharedExpense()
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            val payment =
                viewModel.state.value.payments
                    .first { it.fromUserId == OTHER_DEBTOR }
            viewModel.onPaymentRowClick(payment)
            viewModel.onSettleConfirm("Settlement")
            advanceUntilIdle()

            val settlement =
                assertIs<TabEntry.Settlement>(
                    tabEntryRepo.getTabEntriesForGroup(GROUP_ID).first().last(),
                )
            assertEquals(OTHER_DEBTOR, settlement.paidByUserId)
            assertEquals(PAYEE, settlement.receivedByUserId)
            assertEquals(10.0, settlement.amount)
            assertNull(viewModel.pendingSettlement.value)
            val recorded = assertIs<SettleUpEvent.PaymentRecorded>(events.last())
            assertEquals("Cara", recorded.fromName)
            assertEquals("Bob", recorded.toName)
            assertFalse(recorded.isFromCurrentUser)
        }

    @Test
    fun confirmWithSmallerAmountRecordsPartialSettlement() =
        runTest(testDispatcher) {
            val tabEntryRepo = repoWithSharedExpense()
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            val payment =
                viewModel.state.value.payments
                    .first { it.fromUserId == OTHER_DEBTOR }
            viewModel.onPaymentRowClick(payment)
            viewModel.settleAmountTextState.setTextAndPlaceCursorAtEnd("4")
            viewModel.onSettleConfirm("Settlement")
            advanceUntilIdle()

            val settlement =
                assertIs<TabEntry.Settlement>(
                    tabEntryRepo.getTabEntriesForGroup(GROUP_ID).first().last(),
                )
            assertEquals(4.0, settlement.amount)
            // The remaining debt shrinks to the unpaid part.
            val remaining =
                viewModel.state.value.payments
                    .first { it.fromUserId == OTHER_DEBTOR }
            assertEquals(6.0, remaining.amount)
        }

    @Test
    fun confirmWithInvalidAmountDoesNotCallRepository() =
        runTest(testDispatcher) {
            val tabEntryRepo = repoWithSharedExpense()
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()
            val entriesBefore = tabEntryRepo.getTabEntriesForGroup(GROUP_ID).first().size

            listOf("0", "-2", "10.51", "abc").forEach { input ->
                val payment =
                    viewModel.state.value.payments
                        .first { it.fromUserId == OTHER_DEBTOR }
                viewModel.onPaymentRowClick(payment)
                viewModel.settleAmountTextState.setTextAndPlaceCursorAtEnd(input)
                viewModel.onSettleConfirm("Settlement")
                advanceUntilIdle()
            }

            assertEquals(entriesBefore, tabEntryRepo.getTabEntriesForGroup(GROUP_ID).first().size)
            assertEquals(4, events.filterIsInstance<SettleUpEvent.Error>().size)
            assertNull(viewModel.pendingSettlement.value)
        }

    @Test
    fun settlingOnePairDoesNotBlockAnotherPair() =
        runTest(testDispatcher) {
            val tabEntryRepo = repoWithSharedExpense()
            val gate = CompletableDeferred<Unit>()
            tabEntryRepo.settlementGate = gate
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            val otherPayment =
                viewModel.state.value.payments
                    .first { it.fromUserId == OTHER_DEBTOR }
            viewModel.onPaymentRowClick(otherPayment)
            viewModel.onSettleConfirm("Settlement")
            advanceUntilIdle()

            val payments = viewModel.state.value.payments
            assertTrue(payments.first { it.fromUserId == OTHER_DEBTOR }.isSettling)
            assertFalse(payments.first { it.fromUserId == CURRENT_USER }.isSettling)
            // The busy pair's row click is ignored, the free pair's isn't.
            viewModel.onPaymentRowClick(payments.first { it.fromUserId == OTHER_DEBTOR })
            assertNull(viewModel.pendingSettlement.value)
            viewModel.onPaymentRowClick(payments.first { it.fromUserId == CURRENT_USER })
            assertNotNull(viewModel.pendingSettlement.value)

            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(
                viewModel.state.value.payments
                    .any { it.isSettling },
            )
        }

    @Test
    fun repositoryFailureClearsSettlingAndEmitsError() =
        runTest(testDispatcher) {
            val tabEntryRepo = repoWithSharedExpense()
            tabEntryRepo.settlementError = DataError.Remote.SERVER_ERROR
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            val payment =
                viewModel.state.value.payments
                    .first { it.fromUserId == OTHER_DEBTOR }
            viewModel.onPaymentRowClick(payment)
            viewModel.onSettleConfirm("Settlement")
            advanceUntilIdle()

            assertIs<SettleUpEvent.Error>(events.last())
            assertFalse(
                viewModel.state.value.payments
                    .any { it.isSettling },
            )
        }

    @Test
    fun confirmAfterDebtDisappearedDoesNotCallRepository() =
        runTest(testDispatcher) {
            val tabEntryRepo = repoWithSharedExpense()
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            val payment =
                viewModel.state.value.payments
                    .first { it.fromUserId == OTHER_DEBTOR }
            viewModel.onPaymentRowClick(payment)
            // The debt gets fully settled elsewhere while the dialog is open.
            tabEntryRepo.emit(GROUP_ID, emptyList())
            advanceUntilIdle()
            viewModel.onSettleConfirm("Settlement")
            advanceUntilIdle()

            assertTrue(tabEntryRepo.getTabEntriesForGroup(GROUP_ID).first().isEmpty())
            assertIs<SettleUpEvent.Error>(events.last())
            assertNull(viewModel.pendingSettlement.value)
        }

    @Test
    fun dismissClearsPendingSettlementAndAmount() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(tabEntryRepository = repoWithSharedExpense())
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onPaymentRowClick(
                viewModel.state.value.payments
                    .first(),
            )
            viewModel.onSettleDismiss()

            assertNull(viewModel.pendingSettlement.value)
            assertEquals("", viewModel.settleAmountTextState.text.toString())
        }

    /**
     * One expense of 30 paid by Bob, split equally: Alice (current user) and Cara each
     * owe Bob 10 after simplification.
     */
    private fun repoWithSharedExpense(): FakeTabEntryRepository =
        FakeTabEntryRepository(
            initialEntries =
                mapOf(
                    GROUP_ID to
                        listOf(
                            Fixtures.expense(
                                id = "e1",
                                groupId = GROUP_ID,
                                amount = 30.0,
                                paidByUserId = PAYEE,
                                splits =
                                    listOf(
                                        Fixtures.split("e1", CURRENT_USER, 10.0),
                                        Fixtures.split("e1", PAYEE, 10.0),
                                        Fixtures.split("e1", OTHER_DEBTOR, 10.0),
                                    ),
                            ),
                        ),
                ),
        )

    private fun TestScope.collectEvents(viewModel: SettleUpViewModel): List<SettleUpEvent> {
        val events = mutableListOf<SettleUpEvent>()
        backgroundScope.launch { viewModel.events.collect { events.add(it) } }
        return events
    }

    private fun TestScope.activateState(viewModel: SettleUpViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        groupRepository: FakeGroupRepository =
            FakeGroupRepository(
                initialGroups =
                    listOf(
                        Fixtures.group(
                            id = GROUP_ID,
                            participants =
                                setOf(
                                    Fixtures.participant(CURRENT_USER, "Alice"),
                                    Fixtures.participant(PAYEE, "Bob"),
                                    Fixtures.participant(OTHER_DEBTOR, "Cara"),
                                ),
                            currency = "EUR",
                        ),
                    ),
            ),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        currentAccount: FakeCurrentAccount = FakeCurrentAccount(),
    ): SettleUpViewModel =
        SettleUpViewModel(
            groupId = GROUP_ID,
            tabEntryRepository = tabEntryRepository,
            groupRepository = groupRepository,
            currencyRepository = currencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            currentAccount = currentAccount,
            numberSymbols = NumberSymbols.Fallback,
        )

    private companion object {
        const val GROUP_ID = "g1"
        const val CURRENT_USER = "user-1"
        const val PAYEE = "user-2"
        const val OTHER_DEBTOR = "user-3"
    }
}
