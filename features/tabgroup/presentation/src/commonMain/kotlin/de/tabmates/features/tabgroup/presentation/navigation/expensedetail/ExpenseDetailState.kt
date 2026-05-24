package de.tabmates.features.tabgroup.presentation.navigation.expensedetail

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.TabEntry

data class ExpenseDetailState(
    val expenseId: String = "",
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val expense: TabEntry.Expense? = null,
    val currentUserId: String = "",
    val groupCurrencySymbol: String = "",
    val groupCurrencyDecimalDigits: Int = 2,
    val membersById: Map<String, GroupParticipant> = emptyMap(),
)
