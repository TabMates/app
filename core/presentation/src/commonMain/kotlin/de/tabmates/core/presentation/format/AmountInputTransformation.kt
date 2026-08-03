package de.tabmates.core.presentation.format

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange

/**
 * Keeps an amount field to digits plus a single decimal separator, capped at [decimals] fraction
 * digits. Typing the "wrong" separator is corrected to the locale's one rather than rejected, since
 * hardware keyboards emit a `.` regardless of locale. A *pasted* grouped amount ("1.234,56") is
 * reduced to its plain form instead of being dropped — typing is held to the strict rule so a
 * stray second separator is simply refused.
 */
@Composable
fun rememberAmountInputTransformation(
    symbols: NumberSymbols,
    decimals: Int = DEFAULT_CURRENCY_DECIMALS,
): InputTransformation =
    remember(symbols, decimals) {
        AmountInputTransformation(symbols = symbols, decimals = decimals)
    }

private class AmountInputTransformation(
    private val symbols: NumberSymbols,
    private val decimals: Int,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val text = asCharSequence().toString()
        if (text.isEmpty()) return

        val strict = strictAmountInput(text, symbols, decimals)
        if (strict != null) {
            // Same length by construction — separators are swapped one for one, so per-character
            // edits leave the cursor where the user put it.
            strict.forEachIndexed { index, char ->
                if (text[index] != char) replace(index, index + 1, char.toString())
            }
            return
        }

        val pasted = if (hasBulkInsert()) parseAmount(text, symbols)?.takeIf { it >= 0.0 } else null
        if (pasted == null) {
            revertAllChanges()
            return
        }
        replace(0, length, formatAmountForInput(pasted, decimals, symbols))
        selection = TextRange(length)
    }
}

/** True when this edit added more than one character at once, i.e. a paste rather than typing. */
private fun TextFieldBuffer.hasBulkInsert(): Boolean = length - originalText.length > 1

/**
 * Returns [text] with any decimal separator normalised to the locale's, or null when it is not a
 * valid partial amount. Character count is preserved so callers can patch in place.
 */
internal fun strictAmountInput(
    text: String,
    symbols: NumberSymbols,
    decimals: Int,
): String? {
    val normalized = StringBuilder(text.length)
    var seenSeparator = false
    var fractionDigits = 0
    for (char in text) {
        when {
            char.isDigit() -> {
                if (seenSeparator) {
                    if (fractionDigits >= decimals) return null
                    fractionDigits++
                }
                normalized.append(char)
            }

            char == '.' || char == ',' || char == symbols.decimalSeparator -> {
                if (seenSeparator || decimals <= 0) return null
                seenSeparator = true
                normalized.append(symbols.decimalSeparator)
            }

            else -> {
                return null
            }
        }
    }
    return normalized.toString()
}
