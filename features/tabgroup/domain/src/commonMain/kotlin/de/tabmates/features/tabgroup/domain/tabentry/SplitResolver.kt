package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.features.tabgroup.domain.models.SplitType

/**
 * Resolves per-participant absolute amounts for an expense given the split type and total.
 * Pure function — no IO. Lives in domain so business logic stays out of the data layer.
 */
object SplitResolver {
    fun resolveAmounts(
        splits: List<NewTabEntrySplit>,
        totalAmount: Double,
    ): List<Double> {
        if (splits.isEmpty()) return emptyList()
        val totalShares = splits.sumOf { if (it.splitType == SplitType.SHARES) it.value else 0.0 }
        return splits.map { split ->
            when (split.splitType) {
                SplitType.EQUAL -> totalAmount / splits.size
                SplitType.EXACT_AMOUNT -> split.value
                SplitType.PERCENTAGE -> totalAmount * (split.value / 100.0)
                SplitType.SHARES -> if (totalShares > 0) totalAmount * (split.value / totalShares) else 0.0
            }
        }
    }
}
