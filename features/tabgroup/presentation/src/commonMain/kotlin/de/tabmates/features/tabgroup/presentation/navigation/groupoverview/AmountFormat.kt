package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import de.tabmates.features.tabgroup.presentation.components.formatMoneyUnsigned

internal fun formatAmount(
    item: GroupOverviewItem,
    amount: Double,
): String = formatAmount(amount, item.currencySymbol, item.currencyDecimalDigits)

internal fun formatAmount(
    amount: Double,
    symbol: String,
    decimalDigits: Int,
): String = formatMoneyUnsigned(symbol, amount, decimalDigits)
