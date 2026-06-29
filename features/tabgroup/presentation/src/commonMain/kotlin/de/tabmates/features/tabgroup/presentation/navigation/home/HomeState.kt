package de.tabmates.features.tabgroup.presentation.navigation.home

import de.tabmates.features.tabgroup.domain.models.GroupBalance

data class HomeState(
    val isLoading: Boolean = true,
    // Net balance aggregated across all groups, signed, in the display currency.
    val netAmount: Double = 0.0,
    val displaySymbol: String = "",
    val displayDecimals: Int = 2,
    // Groups that contribute a non-zero balance.
    val contributingGroupCount: Int = 0,
    val topGroups: List<HomeGroup> = emptyList(),
)

data class HomeGroup(
    val id: String,
    val title: String,
    val iconKey: String,
    val colorKey: String,
    val memberCount: Int,
    val balance: GroupBalance,
    val currencySymbol: String,
    val currencyDecimals: Int,
    val memberInitials: List<String>,
    /** True when the group has at least one local expense not yet confirmed by the server. */
    val hasPendingSync: Boolean = false,
)
