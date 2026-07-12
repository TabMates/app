package de.tabmates.features.tabgroup.presentation.navigation.expensedetail

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlin.time.Instant

data class ExpenseDetailState(
    val expenseId: String = "",
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val expense: TabEntry.Expense? = null,
    val currentUserId: String = "",
    // The currency the expense was paid in — amounts on this screen render in it.
    val expenseCurrencyCode: String = "",
    val expenseCurrencySymbol: String = "",
    val expenseCurrencyDecimalDigits: Int = 2,
    // The group's default currency, base for the converted amount and exchange rate.
    val groupCurrencyCode: String = "",
    val groupCurrencySymbol: String = "",
    val groupCurrencyDecimalDigits: Int = 2,
    val ratesByCurrency: Map<String, Double> = emptyMap(),
    val ratesLastUpdatedAt: Instant? = null,
    val membersById: Map<String, GroupParticipant> = emptyMap(),
) {
    /** True when the expense currency differs from the group's default currency. */
    val isForeignCurrency: Boolean
        get() =
            groupCurrencyCode.isNotEmpty() &&
                expenseCurrencyCode.isNotEmpty() &&
                expenseCurrencyCode != groupCurrencyCode
}
