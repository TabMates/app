package de.tabmates.features.tabgroup.database.entities.types

enum class SplitTypeDatabase {
    /**
     * Split equally among all participants.
     */
    EQUAL,

    /**
     * Each participant pays an exact amount.
     * Sum of all exact amounts should equal the total expense amount.
     */
    EXACT_AMOUNT,

    /**
     * Each participant pays a percentage of the total.
     * Sum of all percentages should equal 100.
     */
    PERCENTAGE,

    /**
     * Split by shares/parts (e.g., 2 shares vs 1 share means 2/3 vs 1/3).
     * Each participant pays (theirShares / totalShares) * totalAmount.
     */
    SHARES,
}
