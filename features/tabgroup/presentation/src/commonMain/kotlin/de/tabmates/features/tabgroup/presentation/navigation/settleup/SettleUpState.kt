package de.tabmates.features.tabgroup.presentation.navigation.settleup

data class SettleUpState(
    val groupId: String,
    val currentUserId: String,
    val isLoading: Boolean = true,
    val currencyCode: String = "",
    val currencySymbol: String = "",
    val currencyDecimalDigits: Int = 2,
    val payments: List<SettleUpPayment> = emptyList(),
)

/** One suggested payment between two group members, after smart debt simplification. */
data class SettleUpPayment(
    val fromUserId: String,
    val fromName: String,
    val fromInitials: String,
    val toUserId: String,
    val toName: String,
    val toInitials: String,
    val amount: Double,
    val isSettling: Boolean = false,
)
