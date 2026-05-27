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

/** One suggested outgoing payment from the current user, after smart debt simplification. */
data class SettleUpPayment(
    val toUserId: String,
    val toName: String,
    val toInitials: String,
    val amount: Double,
    val isSettling: Boolean = false,
)
