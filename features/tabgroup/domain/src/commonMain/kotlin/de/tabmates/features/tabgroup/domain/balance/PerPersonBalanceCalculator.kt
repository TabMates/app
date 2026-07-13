package de.tabmates.features.tabgroup.domain.balance

import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.currency.factorFor
import de.tabmates.features.tabgroup.domain.models.TabEntry

/**
 * Computes per-counterparty net balance against [currentUserId].
 *
 * Map key = other participant userId.
 * Positive value = that participant owes the current user.
 * Negative value = the current user owes that participant.
 *
 * Deleted entries must be filtered by the caller. When [conversion] is provided, amounts are
 * converted into the group's base currency; entries with an unknown rate are skipped.
 */
object PerPersonBalanceCalculator {
    fun computeByParticipant(
        entries: List<TabEntry>,
        currentUserId: String,
        conversion: CurrencyConversion? = null,
    ): Map<String, Double> {
        if (currentUserId.isEmpty()) return emptyMap()
        val net = mutableMapOf<String, Double>()
        entries.forEach { entry ->
            val factor = conversion.factorFor(entry) ?: return@forEach
            when (entry) {
                is TabEntry.Expense -> {
                    entry.splits.forEach { split ->
                        when {
                            entry.paidByUserId == currentUserId && split.participantId != currentUserId -> {
                                net.addTo(split.participantId, factor * split.resolvedAmount)
                            }

                            entry.paidByUserId != currentUserId && split.participantId == currentUserId -> {
                                net.addTo(entry.paidByUserId, -factor * split.resolvedAmount)
                            }
                        }
                    }
                }

                is TabEntry.Income -> {
                    entry.splits.forEach { split ->
                        when {
                            entry.paidByUserId == currentUserId && split.participantId != currentUserId -> {
                                net.addTo(split.participantId, -factor * split.resolvedAmount)
                            }

                            entry.paidByUserId != currentUserId && split.participantId == currentUserId -> {
                                net.addTo(entry.paidByUserId, factor * split.resolvedAmount)
                            }
                        }
                    }
                }

                is TabEntry.Settlement -> {
                    if (entry.paidByUserId == currentUserId && entry.receivedByUserId != currentUserId) {
                        net.addTo(entry.receivedByUserId, factor * entry.amount)
                    } else if (entry.receivedByUserId == currentUserId && entry.paidByUserId != currentUserId) {
                        net.addTo(entry.paidByUserId, -factor * entry.amount)
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
