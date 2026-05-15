package de.tabmates.features.tabgroup.domain.balance

import de.tabmates.features.tabgroup.domain.models.TabEntry

/**
 * Computes per-counterparty net balance against [currentUserId].
 *
 * Map key = other participant userId.
 * Positive value = that participant owes the current user.
 * Negative value = the current user owes that participant.
 *
 * Deleted entries must be filtered by the caller.
 */
object PerPersonBalanceCalculator {
    fun computeByParticipant(
        entries: List<TabEntry>,
        currentUserId: String,
    ): Map<String, Double> {
        if (currentUserId.isEmpty()) return emptyMap()
        val net = mutableMapOf<String, Double>()
        entries.forEach { entry ->
            when (entry) {
                is TabEntry.Expense -> {
                    entry.splits.forEach { split ->
                        when {
                            entry.paidByUserId == currentUserId && split.participantId != currentUserId -> {
                                net.addTo(split.participantId, split.resolvedAmount)
                            }

                            entry.paidByUserId != currentUserId && split.participantId == currentUserId -> {
                                net.addTo(entry.paidByUserId, -split.resolvedAmount)
                            }
                        }
                    }
                }

                is TabEntry.Income -> {
                    entry.splits.forEach { split ->
                        when {
                            entry.paidByUserId == currentUserId && split.participantId != currentUserId -> {
                                net.addTo(split.participantId, -split.resolvedAmount)
                            }

                            entry.paidByUserId != currentUserId && split.participantId == currentUserId -> {
                                net.addTo(entry.paidByUserId, split.resolvedAmount)
                            }
                        }
                    }
                }

                is TabEntry.Settlement -> {
                    if (entry.paidByUserId == currentUserId && entry.receivedByUserId != currentUserId) {
                        net.addTo(entry.receivedByUserId, entry.amount)
                    } else if (entry.receivedByUserId == currentUserId && entry.paidByUserId != currentUserId) {
                        net.addTo(entry.paidByUserId, -entry.amount)
                    }
                }
            }
        }
        return net
    }

    private fun MutableMap<String, Double>.addTo(
        key: String,
        delta: Double,
    ) {
        this[key] = (this[key] ?: 0.0) + delta
    }
}
