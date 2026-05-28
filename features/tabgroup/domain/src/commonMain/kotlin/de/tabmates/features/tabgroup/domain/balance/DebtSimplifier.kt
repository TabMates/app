package de.tabmates.features.tabgroup.domain.balance

import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.models.TabEntry

/** A single suggested payment in a simplified debt graph: [fromUserId] pays [toUserId] [amount]. */
data class SimplifiedDebt(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double,
)

/**
 * Minimizes the number and size of transfers needed to settle a group.
 *
 * Instead of every debtor paying back each person they directly owe, the whole group is reduced to
 * its net positions and matched greedily (largest creditor to largest debtor). The total money that
 * changes hands equals the sum of all positive net balances — the theoretical minimum — and each
 * member pays as few people as possible.
 */
object DebtSimplifier {
    /**
     * Builds the simplified payment plan for an entire group.
     *
     * @param netByUser net balance per user — positive = owed money (creditor), negative = owes
     *   money (debtor). Values within [epsilon] of zero are treated as settled.
     * @param epsilon smallest amount worth settling; derive from the currency's decimal digits
     *   (e.g. `0.005` for 2-decimal currencies) so sub-cent residuals are dropped.
     */
    fun simplify(
        netByUser: Map<String, Double>,
        epsilon: Double = DEFAULT_EPSILON,
    ): List<SimplifiedDebt> {
        // Deterministic order (amount desc, then id) so the plan is stable across recomputes.
        val creditors =
            netByUser
                .filter { it.value > epsilon }
                .map { Holder(it.key, it.value) }
                .sortedWith(compareByDescending<Holder> { it.amount }.thenBy { it.id })
                .toMutableList()
        val debtors =
            netByUser
                .filter { it.value < -epsilon }
                .map { Holder(it.key, -it.value) }
                .sortedWith(compareByDescending<Holder> { it.amount }.thenBy { it.id })
                .toMutableList()

        val result = mutableListOf<SimplifiedDebt>()
        var ci = 0
        var di = 0
        while (ci < creditors.size && di < debtors.size) {
            val creditor = creditors[ci]
            val debtor = debtors[di]
            val pay = minOf(creditor.amount, debtor.amount)
            if (pay > epsilon) {
                result +=
                    SimplifiedDebt(
                        fromUserId = debtor.id,
                        toUserId = creditor.id,
                        amount = pay,
                    )
            }
            creditor.amount -= pay
            debtor.amount -= pay
            if (creditor.amount <= epsilon) ci++
            if (debtor.amount <= epsilon) di++
        }
        return result
    }

    /**
     * Convenience over [simplify] that derives net positions from the group's [entries].
     *
     * @param participantIds every member of the group (so members with only one-sided activity are
     *   still considered).
     */
    fun simplifyFromEntries(
        entries: List<TabEntry>,
        participantIds: Collection<String>,
        conversion: CurrencyConversion? = null,
        epsilon: Double = DEFAULT_EPSILON,
    ): List<SimplifiedDebt> {
        val activeEntries = entries.filterNot { it.isDeleted }
        val netByUser =
            participantIds.associateWith { userId ->
                UserBalanceCalculator.computeNet(activeEntries, userId, conversion)
            }
        return simplify(netByUser, epsilon)
    }

    private const val DEFAULT_EPSILON = 0.005

    private class Holder(
        val id: String,
        var amount: Double,
    )
}
