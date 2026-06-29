package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import de.tabmates.features.tabgroup.domain.balance.UserBalanceCalculator
import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.presentation.components.deriveGroupAvatarKey

internal fun Group.toUiItem(currency: Currency?): GroupOverviewItem {
    val (iconKey, colorKey) = deriveGroupAvatarKey(id)
    return GroupOverviewItem(
        id = id,
        title = title,
        iconKey = iconKey,
        colorKey = colorKey,
        memberCount = participants.size,
        expenseCount = 0,
        totalSpent = 0.0,
        balance = GroupBalance.Settled,
        currencyCode = defaultCurrencyCode,
        currencySymbol = currency?.nativeSymbol ?: defaultCurrencyCode,
        currencyDecimalDigits = currency?.decimalDigits ?: 2,
        lastActivityAt = lastActivityAt,
        inviteToken = inviteToken,
        creatorUserId = creator.userId,
    )
}

internal fun GroupOverviewItem.withStats(
    entries: List<TabEntry>,
    currentUserId: String,
    conversion: CurrencyConversion? = null,
): GroupOverviewItem {
    val visibleEntries = entries.filterNot { it.isDeleted }
    val expenseCount = visibleEntries.count { it is TabEntry.Expense }
    val totalSpent =
        visibleEntries.filterIsInstance<TabEntry.Expense>().sumOf { expense ->
            val factor = if (conversion == null) 1.0 else conversion.factorToBase(expense.currencyCode) ?: 0.0
            expense.amount * factor
        }
    val net = UserBalanceCalculator.computeNet(visibleEntries, currentUserId, conversion)
    return copy(
        expenseCount = expenseCount,
        totalSpent = totalSpent,
        balance = GroupBalance.fromNet(net),
        hasPendingSync = entries.any { it.isPendingSync },
    )
}
