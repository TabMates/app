package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import de.tabmates.core.presentation.format.AmountSign
import de.tabmates.core.presentation.format.LocalNumberSymbols
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.core.presentation.format.formatMoneyUnsigned
import de.tabmates.core.presentation.format.formatSignedMoney

/**
 * Composable shorthands over the shared money formatter: they pull the locale's [NumberSymbols] out
 * of the composition so screens don't have to thread them through. Non-composable helpers take
 * [numberSymbols] explicitly.
 */
@Composable
@ReadOnlyComposable
internal fun formatAmount(
    item: GroupOverviewItem,
    amount: Double,
): String = formatAmount(amount, item.currencySymbol, item.currencyDecimalDigits)

@Composable
@ReadOnlyComposable
internal fun formatAmount(
    amount: Double,
    symbol: String,
    decimalDigits: Int,
): String = formatAmount(amount, symbol, decimalDigits, LocalNumberSymbols.current)

internal fun formatAmount(
    amount: Double,
    symbol: String,
    decimalDigits: Int,
    numberSymbols: NumberSymbols,
): String = formatMoneyUnsigned(symbol, amount, decimalDigits, numberSymbols)

/** Balance amounts, where the sign comes from the debt direction rather than from the number. */
@Composable
@ReadOnlyComposable
internal fun formatSignedAmount(
    item: GroupOverviewItem,
    amount: Double,
    sign: AmountSign,
): String = formatSignedAmount(amount, item.currencySymbol, item.currencyDecimalDigits, sign)

@Composable
@ReadOnlyComposable
internal fun formatSignedAmount(
    amount: Double,
    symbol: String,
    decimalDigits: Int,
    sign: AmountSign,
): String = formatSignedMoney(symbol, amount, decimalDigits, LocalNumberSymbols.current, sign)
