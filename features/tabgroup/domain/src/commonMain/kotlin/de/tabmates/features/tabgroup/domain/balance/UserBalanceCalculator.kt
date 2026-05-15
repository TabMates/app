package de.tabmates.features.tabgroup.domain.balance

import de.tabmates.features.tabgroup.domain.models.TabEntry

/**
 * Computes the net balance for [userId] across a list of [TabEntry] within a single group.
 *
 * Positive result = user is owed money. Negative = user owes money.
 * Deleted entries must be filtered by the caller.
 */
object UserBalanceCalculator {
    fun computeNet(
        entries: List<TabEntry>,
        userId: String,
    ): Double {
        if (userId.isEmpty()) return 0.0
        var net = 0.0
        entries.forEach { entry ->
            when (entry) {
                is TabEntry.Expense -> {
                    val myShare = entry.splits.firstOrNull { it.participantId == userId }?.resolvedAmount ?: 0.0
                    net +=
                        if (entry.paidByUserId == userId) {
                            entry.amount - myShare
                        } else {
                            -myShare
                        }
                }

                is TabEntry.Income -> {
                    val myShare = entry.splits.firstOrNull { it.participantId == userId }?.resolvedAmount ?: 0.0
                    net +=
                        if (entry.paidByUserId == userId) {
                            myShare - entry.amount
                        } else {
                            myShare
                        }
                }

                is TabEntry.Settlement -> {
                    if (entry.paidByUserId == userId) net += entry.amount
                    if (entry.receivedByUserId == userId) net -= entry.amount
                }
            }
        }
        return net
    }
}
