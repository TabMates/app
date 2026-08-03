package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import de.tabmates.core.presentation.format.LocalNumberSymbols
import de.tabmates.core.presentation.format.formatMoney
import de.tabmates.core.presentation.format.formatMoneyUnsigned
import de.tabmates.core.presentation.format.formatPercent
import de.tabmates.core.presentation.format.formatRate
import de.tabmates.core.presentation.format.parseAmount
import de.tabmates.core.presentation.format.rememberAmountInputTransformation

/**
 * Composable shorthands over the shared formatters in `:core:presentation`, which take the locale's
 * `NumberSymbols` explicitly. Screens read them out of the composition instead; ViewModels get the
 * same values injected, since they cannot read a CompositionLocal.
 */
@Composable
@ReadOnlyComposable
internal fun formatMoney(
    symbol: String,
    amount: Double,
    decimals: Int,
): String = formatMoney(symbol, amount, decimals, LocalNumberSymbols.current)

@Composable
@ReadOnlyComposable
internal fun formatMoneyUnsigned(
    symbol: String,
    amount: Double,
    decimals: Int,
): String = formatMoneyUnsigned(symbol, amount, decimals, LocalNumberSymbols.current)

/** Includes the locale's percent symbol and spacing — callers must not append their own `%`. */
@Composable
@ReadOnlyComposable
internal fun formatPercent(value: Double): String = formatPercent(value, LocalNumberSymbols.current)

@Composable
@ReadOnlyComposable
internal fun formatRate(rate: Double): String? = formatRate(rate, LocalNumberSymbols.current)

/** Reads what the user typed in the locale's notation. See the core `parseAmount` for the rules. */
@Composable
@ReadOnlyComposable
internal fun parseAmount(input: String): Double? = parseAmount(input, LocalNumberSymbols.current)

/** Filter for amount fields, capped at [decimals] fraction digits. */
@Composable
internal fun rememberAmountInputTransformation(decimals: Int): InputTransformation =
    rememberAmountInputTransformation(symbols = LocalNumberSymbols.current, decimals = decimals)
