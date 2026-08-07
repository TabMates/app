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

/**
 * One suggested payment between two group members, after smart debt simplification.
 *
 * Either side may be a former member: removal keeps their expenses, so their debt is still part of
 * the plan and dropping it would stop the numbers adding up.
 */
data class SettleUpPayment(
    val fromUserId: String,
    val fromName: String,
    val fromInitials: String,
    val isFromFormerMember: Boolean = false,
    val toUserId: String,
    val toName: String,
    val toInitials: String,
    val isToFormerMember: Boolean = false,
    val amount: Double,
    val isSettling: Boolean = false,
)
