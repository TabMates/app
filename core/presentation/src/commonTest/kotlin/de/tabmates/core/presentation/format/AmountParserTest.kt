package de.tabmates.core.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmountParserTest {
    @Test
    fun readsTheLocaleDecimalSeparator() {
        assertEquals(12.5, parseAmount("12,5", DE_DE))
        assertEquals(12.5, parseAmount("12.5", EN_US))
    }

    @Test
    fun toleratesTheOtherSeparatorFromAHardwareKeyboard() {
        assertEquals(1.5, parseAmount("1.5", DE_DE))
        assertEquals(1.5, parseAmount("1,5", EN_US))
    }

    @Test
    fun aFullGroupAfterTheSeparatorMeansGrouping() {
        assertEquals(1234.0, parseAmount("1.234", DE_DE))
        assertEquals(1234.0, parseAmount("1,234", EN_US))
    }

    @Test
    fun pastedGroupedAmountsResolve() {
        assertEquals(1234.56, parseAmount("1.234,56", DE_DE))
        assertEquals(1234.56, parseAmount("1,234.56", EN_US))
        assertEquals(1234567.89, parseAmount("1.234.567,89", DE_DE))
    }

    @Test
    fun spaceGroupingSeparatorsAreStripped() {
        assertEquals(1234.56, parseAmount("1" + NARROW_NBSP + "234,56", FR_FR))
        assertEquals(1234.56, parseAmount(" 1 234,56 ", FR_FR))
        assertEquals(1234.56, parseAmount("1" + NBSP + "234,56", FR_FR))
    }

    @Test
    fun negativesAreRead() {
        assertEquals(-5.0, parseAmount("-5,00", DE_DE))
        assertEquals(-1234.5, parseAmount("-1,234.50", EN_US))
    }

    @Test
    fun partialInputIsStillANumber() {
        assertEquals(0.5, parseAmount(".5", EN_US))
        assertEquals(5.0, parseAmount("5.", EN_US))
        assertEquals(12.0, parseAmount("12,", DE_DE))
    }

    @Test
    fun nonNumericInputIsRejected() {
        assertNull(parseAmount("", EN_US))
        assertNull(parseAmount("   ", EN_US))
        assertNull(parseAmount("abc", EN_US))
        assertNull(parseAmount("12€", EN_US))
        assertNull(parseAmount("-", EN_US))
    }
}
