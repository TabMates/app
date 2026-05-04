package de.tabmates.features.tabgroup.presentation.navigation.group

data class GroupListItem(
    val id: String,
    val title: String,
    val memberCount: Int,
    val memberInitials: List<String>,
    val extraMembers: Int,
    val currencySymbol: String,
    val balance: GroupBalance,
)

sealed interface GroupBalance {
    data object Settled : GroupBalance

    data class Owed(
        val amount: Double,
    ) : GroupBalance

    data class Owe(
        val amount: Double,
    ) : GroupBalance
}
