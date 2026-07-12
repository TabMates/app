package de.tabmates.features.tabgroup.domain.balance

import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.models.TabEntry

/**
 * Computes the net balance for [userId] across a list of [TabEntry] within a single group.
 *
 * Positive result = user is owed money. Negative = user owes money.
 * Deleted entries must be filtered by the caller.
 *
 * When [conversion] is provided, each entry's amounts are converted from the entry's own currency
 * into the group's base currency. Entries whose currency has no known rate are skipped (they
 * contribute nothing until rates sync). With [conversion] `null`, amounts are summed as-is
 * (single-currency groups).
 */
object UserBalanceCalculator {
    fun computeNet(
        entries: List<TabEntry>,
        userId: String,
        conversion: CurrencyConversion? = null,
    ): Double {
        if (userId.isEmpty()) return 0.0
        var net = 0.0
        entries.forEach { entry ->
            val factor = conversionFactor(conversion, entry.currencyCode) ?: return@forEach
            net += factor * entryNet(entry, userId)
        }
        return net
    }

    /**
     * Net effect of a single [entry] on [userId]'s balance, in the entry's own currency.
     * Positive = the entry earns the user money, negative = the user owes for it.
     */
    fun entryNet(
        entry: TabEntry,
        userId: String,
    ): Double {
        if (userId.isEmpty()) return 0.0
        return when (entry) {
            is TabEntry.Expense -> {
                val myShare = entry.splits.firstOrNull { it.participantId == userId }?.resolvedAmount ?: 0.0
                if (entry.paidByUserId == userId) entry.amount - myShare else -myShare
            }

            is TabEntry.Income -> {
                val myShare = entry.splits.firstOrNull { it.participantId == userId }?.resolvedAmount ?: 0.0
                if (entry.paidByUserId == userId) myShare - entry.amount else myShare
            }

            is TabEntry.Settlement -> {
                var net = 0.0
                if (entry.paidByUserId == userId) net += entry.amount
                if (entry.receivedByUserId == userId) net -= entry.amount
                net
            }
        }
    }
}

/**
 * Multiplier to convert [currencyCode] into the conversion's base, `1.0` when no conversion is
 * requested, or `null` when the rate is unknown (so the caller can skip the entry).
 */
internal fun conversionFactor(
    conversion: CurrencyConversion?,
    currencyCode: String,
): Double? = if (conversion == null) 1.0 else conversion.factorToBase(currencyCode)
