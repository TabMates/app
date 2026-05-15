package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import kotlin.time.Instant

data class GroupOverviewState(
    val isLoading: Boolean = true,
    val allItems: List<GroupOverviewItem> = emptyList(),
    val displayedItems: List<GroupOverviewItem> = emptyList(),
    val filter: GroupFilter = GroupFilter.ALL,
    val searchQueryState: TextFieldState = TextFieldState(),
    val selectedGroupId: String? = null,
    val currentUserInitials: String = "",
) {
    val selectedItem: GroupOverviewItem?
        get() = allItems.firstOrNull { it.id == selectedGroupId }
}

enum class GroupFilter { ALL, ACTIVE, SETTLED }

data class GroupOverviewItem(
    val id: String,
    val title: String,
    val iconKey: String,
    val colorKey: String,
    val memberCount: Int,
    val expenseCount: Int,
    val totalSpent: Double,
    val balance: GroupBalance,
    val currencyCode: String,
    val currencySymbol: String,
    val currencyDecimalDigits: Int,
    val lastActivityAt: Instant,
)
