package de.tabmates.features.tabgroup.domain.balance

import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DebtSimplifierTest {
    // region basics

    @Test
    fun emptyInputProducesNoPayments() {
        assertTrue(DebtSimplifier.simplify(emptyMap()).isEmpty())
    }

    @Test
    fun settledGroupProducesNoPayments() {
        val plan = DebtSimplifier.simplify(mapOf("a" to 0.0, "b" to 0.0, "c" to 0.0))
        assertTrue(plan.isEmpty())
    }

    @Test
    fun singleDebtorPaysSingleCreditor() {
        val plan = DebtSimplifier.simplify(mapOf("a" to -10.0, "b" to 10.0))
        assertEquals(SimplifiedDebt(fromUserId = "a", toUserId = "b", amount = 10.0), plan.single())
    }

    @Test
    fun reroutesSoMiddlemanPaysNothing() {
        // a owes b 10; c owes a 10. a nets to zero, so the smart plan is just c -> b 10.
        val plan = DebtSimplifier.simplify(mapOf("a" to 0.0, "b" to 10.0, "c" to -10.0))
        assertEquals(listOf(SimplifiedDebt(fromUserId = "c", toUserId = "b", amount = 10.0)), plan)
    }

    @Test
    fun debtorIsSplitAcrossMultipleCreditorsByLargestFirst() {
        val plan = DebtSimplifier.simplify(mapOf("a" to -30.0, "b" to 20.0, "c" to 10.0))
        assertEquals(
            listOf(
                SimplifiedDebt(fromUserId = "a", toUserId = "b", amount = 20.0),
                SimplifiedDebt(fromUserId = "a", toUserId = "c", amount = 10.0),
            ),
            plan,
        )
    }

    @Test
    fun manyDebtorsPayOneCreditorLargestDebtorFirst() {
        val plan = DebtSimplifier.simplify(mapOf("a" to 30.0, "b" to -20.0, "c" to -10.0))
        assertEquals(
            listOf(
                SimplifiedDebt(fromUserId = "b", toUserId = "a", amount = 20.0),
                SimplifiedDebt(fromUserId = "c", toUserId = "a", amount = 10.0),
            ),
            plan,
        )
    }

    // endregion

    // region degenerate / imbalanced inputs

    @Test
    fun creditorWithNoDebtorProducesNothing() {
        assertTrue(DebtSimplifier.simplify(mapOf("a" to 10.0)).isEmpty())
    }

    @Test
    fun debtorWithNoCreditorProducesNothing() {
        assertTrue(DebtSimplifier.simplify(mapOf("a" to -10.0)).isEmpty())
    }

    @Test
    fun imbalancedInputNeverOverpaysEitherSide() {
        // Debtors total 20 but creditors are owed 30 (input does not sum to zero).
        val net = mapOf("a" to -20.0, "b" to 30.0)
        val plan = DebtSimplifier.simplify(net)
        // Only the matchable 20 moves.
        assertEquals(20.0, plan.sumOf { it.amount }, absoluteTolerance = 0.001)
        assertNoOneOverpaysOrOverReceives(net, plan)
    }

    @Test
    fun multipleDebtorsAndCreditorsImbalancedStillSafe() {
        val net = mapOf("a" to -50.0, "b" to -10.0, "c" to 30.0, "d" to 5.0)
        val plan = DebtSimplifier.simplify(net)
        // Creditor side is the bottleneck: 35 owed total.
        assertEquals(35.0, plan.sumOf { it.amount }, absoluteTolerance = 0.001)
        assertNoOneOverpaysOrOverReceives(net, plan)
        assertAllTransfersWellFormed(net, plan)
    }

    // endregion

    // region invariants

    @Test
    fun planFullySettlesEveryoneWhenBalanced() {
        val net = mapOf("a" to -25.0, "b" to -5.0, "c" to 20.0, "d" to 10.0)
        val residual = applyPlan(net, DebtSimplifier.simplify(net))
        residual.values.forEach { assertTrue(abs(it) < 0.005, "residual $it") }
    }

    @Test
    fun totalMovedEqualsSumOfPositiveBalances() {
        val net = mapOf("a" to -25.0, "b" to -5.0, "c" to 20.0, "d" to 10.0)
        val totalMoved = DebtSimplifier.simplify(net).sumOf { it.amount }
        assertEquals(30.0, totalMoved, absoluteTolerance = 0.001)
    }

    @Test
    fun transactionCountNeverExceedsParticipantsMinusOne() {
        val net = mapOf("a" to -10.0, "b" to -10.0, "c" to -10.0, "d" to 30.0)
        val plan = DebtSimplifier.simplify(net)
        assertTrue(plan.size <= 3, "expected <= 3 transfers, got ${plan.size}")
        applyPlan(net, plan).values.forEach { assertTrue(abs(it) < 0.005) }
    }

    @Test
    fun deterministicAcrossRepeatedCalls() {
        val net = mapOf("a" to -10.0, "b" to -5.0, "c" to 8.0, "d" to 7.0)
        assertEquals(DebtSimplifier.simplify(net), DebtSimplifier.simplify(net))
    }

    @Test
    fun tieBreaksByIdWhenAmountsAreEqual() {
        // Two equal creditors; deterministic order is largest-then-id, so "b" is paid before "c".
        val plan = DebtSimplifier.simplify(mapOf("a" to -20.0, "c" to 10.0, "b" to 10.0))
        assertEquals(
            listOf(
                SimplifiedDebt(fromUserId = "a", toUserId = "b", amount = 10.0),
                SimplifiedDebt(fromUserId = "a", toUserId = "c", amount = 10.0),
            ),
            plan,
        )
    }

    // endregion

    // region epsilon / rounding

    @Test
    fun subEpsilonResidualsAreDropped() {
        assertTrue(DebtSimplifier.simplify(mapOf("a" to -0.004, "b" to 0.004)).isEmpty())
    }

    @Test
    fun customEpsilonDropsResidualsBelowThreshold() {
        // Whole-number currency: epsilon 0.5 means anything under half a unit is ignored.
        assertTrue(DebtSimplifier.simplify(mapOf("a" to -0.4, "b" to 0.4), epsilon = 0.5).isEmpty())
        assertEquals(1, DebtSimplifier.simplify(mapOf("a" to -1.0, "b" to 1.0), epsilon = 0.5).size)
    }

    @Test
    fun centSizedBalancesAreSettledExactly() {
        val net = mapOf("a" to -0.01, "b" to -0.02, "c" to 0.03)
        val plan = DebtSimplifier.simplify(net)
        assertEquals(0.03, plan.sumOf { it.amount }, absoluteTolerance = 0.0001)
        applyPlan(net, plan).values.forEach { assertTrue(abs(it) < 0.005) }
    }

    // endregion

    // region large groups (property-style)

    @Test
    fun largeBalancedGroupIsFullySettledWithMinimalTransfers() {
        repeat(20) { iteration ->
            val random = Random(seed = iteration.toLong())
            val net = balancedNet(memberCount = 150, random = random)
            val plan = DebtSimplifier.simplify(net)

            // Everyone ends at zero.
            applyPlan(net, plan).values.forEach {
                assertTrue(abs(it) < 0.01, "iteration $iteration residual $it")
            }
            // Minimal-transfer guarantee: at most (nonzero participants - 1) transfers.
            val nonZero = net.values.count { abs(it) > 0.005 }
            assertTrue(
                plan.size <= (nonZero - 1).coerceAtLeast(0),
                "iteration $iteration: ${plan.size} transfers for $nonZero non-zero members",
            )
            // Money conservation: total moved equals what creditors are owed.
            val sumPositive = net.values.filter { it > 0 }.sum()
            assertEquals(sumPositive, plan.sumOf { it.amount }, absoluteTolerance = 0.05)
            assertAllTransfersWellFormed(net, plan)
        }
    }

    @Test
    fun starShapedGroupOneCreditorManyDebtors() {
        val debtors = (0 until 99).associate { "d$it" to -10.0 }
        val net = debtors + ("creditor" to 990.0)
        val plan = DebtSimplifier.simplify(net)
        assertEquals(99, plan.size)
        assertTrue(plan.all { it.toUserId == "creditor" })
        applyPlan(net, plan).values.forEach { assertTrue(abs(it) < 0.005) }
    }

    // endregion

    // region simplifyFromEntries integration

    @Test
    fun simplifyFromEntriesNetsExpenseSplits() {
        // a paid 30, split equally across a/b/c -> b and c each owe a 10.
        val entry = equalExpense(paidBy = "a", amount = 30.0, members = listOf("a", "b", "c"))
        val plan = DebtSimplifier.simplifyFromEntries(listOf(entry), listOf("a", "b", "c"))
        assertEquals(
            listOf(
                SimplifiedDebt(fromUserId = "b", toUserId = "a", amount = 10.0),
                SimplifiedDebt(fromUserId = "c", toUserId = "a", amount = 10.0),
            ),
            plan,
        )
    }

    @Test
    fun simplifyFromEntriesCancelsReciprocalExpenses() {
        // a buys lunch (30, split 3 ways); b buys the same back -> nets cancel.
        val first = equalExpense(paidBy = "a", amount = 30.0, members = listOf("a", "b", "c"), id = "e1")
        val second = equalExpense(paidBy = "b", amount = 30.0, members = listOf("a", "b", "c"), id = "e2")
        val plan = DebtSimplifier.simplifyFromEntries(listOf(first, second), listOf("a", "b", "c"))
        // a is now even, b owes a nothing; only c's two 10s remain (one to a, one to b).
        assertEquals(20.0, plan.sumOf { it.amount }, absoluteTolerance = 0.001)
        assertTrue(plan.all { it.fromUserId == "c" })
    }

    @Test
    fun simplifyFromEntriesIgnoresDeletedEntries() {
        val deleted =
            equalExpense(paidBy = "a", amount = 30.0, members = listOf("a", "b", "c"))
                .copy(
                    deletedAt = Instant.fromEpochMilliseconds(1),
                    deletedByUserId = "a",
                )
        val plan = DebtSimplifier.simplifyFromEntries(listOf(deleted), listOf("a", "b", "c"))
        assertTrue(plan.isEmpty())
    }

    // endregion

    // region helpers

    private fun applyPlan(
        net: Map<String, Double>,
        plan: List<SimplifiedDebt>,
    ): Map<String, Double> {
        val result = net.toMutableMap()
        plan.forEach { debt ->
            // Paying raises the debtor toward zero and lowers the creditor toward zero.
            result[debt.fromUserId] = (result[debt.fromUserId] ?: 0.0) + debt.amount
            result[debt.toUserId] = (result[debt.toUserId] ?: 0.0) - debt.amount
        }
        return result
    }

    private fun assertAllTransfersWellFormed(
        net: Map<String, Double>,
        plan: List<SimplifiedDebt>,
    ) {
        plan.forEach { debt ->
            assertNotEquals(debt.fromUserId, debt.toUserId, "self-payment in $debt")
            assertTrue(debt.amount > 0, "non-positive amount in $debt")
            assertTrue((net[debt.fromUserId] ?: 0.0) < 0, "payer ${debt.fromUserId} is not a debtor")
            assertTrue((net[debt.toUserId] ?: 0.0) > 0, "payee ${debt.toUserId} is not a creditor")
        }
    }

    private fun assertNoOneOverpaysOrOverReceives(
        net: Map<String, Double>,
        plan: List<SimplifiedDebt>,
    ) {
        val paid = plan.groupBy { it.fromUserId }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val received = plan.groupBy { it.toUserId }.mapValues { (_, v) -> v.sumOf { it.amount } }
        paid.forEach { (id, amount) ->
            assertTrue(amount <= -(net[id] ?: 0.0) + 0.001, "$id paid $amount but only owes ${-(net[id] ?: 0.0)}")
        }
        received.forEach { (id, amount) ->
            assertTrue(amount <= (net[id] ?: 0.0) + 0.001, "$id received $amount but is owed ${net[id]}")
        }
    }

    private fun balancedNet(
        memberCount: Int,
        random: Random,
    ): Map<String, Double> {
        val ids = (0 until memberCount).map { "u$it" }
        val net = ids.dropLast(1).associateWith { round2(random.nextDouble(-500.0, 500.0)) }.toMutableMap()
        // Force the books to balance so the group is fully settleable.
        net[ids.last()] = round2(-net.values.sum())
        return net
    }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

    private fun equalExpense(
        paidBy: String,
        amount: Double,
        members: List<String>,
        id: String = "e1",
    ): TabEntry.Expense {
        val share = amount / members.size
        val timestamp = Instant.fromEpochMilliseconds(0)
        return TabEntry.Expense(
            tabEntryId = id,
            groupId = "g",
            title = "",
            description = "",
            amount = amount,
            currencyCode = "EUR",
            creatorId = paidBy,
            paidByUserId = paidBy,
            createdAt = timestamp,
            lastModifiedAt = timestamp,
            lastModifiedByUserId = paidBy,
            version = 0,
            deletedAt = null,
            deletedByUserId = null,
            splits =
                members.mapIndexed { index, memberId ->
                    TabEntrySplit(
                        splitId = "$id-s$index",
                        tabEntryId = id,
                        participantId = memberId,
                        splitType = SplitType.EQUAL,
                        value = share,
                        resolvedAmount = share,
                    )
                },
        )
    }

    // endregion
}
