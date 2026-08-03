package de.tabmates.core.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the rule the amount [rememberAmountInputTransformation] applies while typing. A rejected
 * (null) result is what sends the transformation to its paste fallback, or to reverting the edit.
 */
class AmountInputTest {
    @Test
    fun digitsAndOneSeparatorAreAccepted() {
        assertEquals("12", strictAmountInput("12", EN_US, 2))
        assertEquals("12.5", strictAmountInput("12.5", EN_US, 2))
        assertEquals("12,5", strictAmountInput("12,5", DE_DE, 2))
    }

    @Test
    fun theWrongSeparatorIsRewrittenRatherThanRefused() {
        assertEquals("12,5", strictAmountInput("12.5", DE_DE, 2))
        assertEquals("12.5", strictAmountInput("12,5", EN_US, 2))
    }

    @Test
    fun aTrailingSeparatorIsAllowedSoTheUserCanKeepTyping() {
        assertEquals("12,", strictAmountInput("12,", DE_DE, 2))
    }

    @Test
    fun aSecondSeparatorIsRefused() {
        assertNull(strictAmountInput("12,,5", DE_DE, 2))
        assertNull(strictAmountInput("1.234,56", DE_DE, 2))
    }

    @Test
    fun fractionDigitsAreCappedByTheCurrency() {
        assertNull(strictAmountInput("12.555", EN_US, 2))
        assertNull(strictAmountInput("12.5", EN_US, 0))
        assertEquals("12", strictAmountInput("12", EN_US, 0))
    }

    @Test
    fun anythingElseIsRefused() {
        assertNull(strictAmountInput("abc", EN_US, 2))
        assertNull(strictAmountInput("12€", EN_US, 2))
        assertNull(strictAmountInput("-12", EN_US, 2))
    }

    @Test
    fun aRefusedPasteStillResolvesThroughTheParser() {
        // What the transformation falls back to for a bulk insert.
        val pasted = parseAmount("1.234,56", DE_DE)
        assertEquals("1234,56", formatAmountForInput(pasted!!, 2, DE_DE))
    }
}
