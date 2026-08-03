package de.tabmates.core.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberProbesTest {
    @Test
    fun leadingMarkerIsDetected() {
        assertEquals(Affix(before = true, spacing = ""), affixOf("¤1.00", CURRENCY_MARKER))
    }

    @Test
    fun trailingMarkerCarriesItsSpacing() {
        val trailing = affixOf("1,00" + NBSP + "¤", CURRENCY_MARKER)
        assertEquals(Affix(before = false, spacing = NBSP.toString()), trailing)
        assertEquals(Affix(before = false, spacing = ""), affixOf("50%", "%"))
    }

    @Test
    fun missingMarkerFallsBackToLeading() {
        assertEquals(Affix(before = true, spacing = ""), affixOf("1.00", CURRENCY_MARKER))
    }

    @Test
    fun uniformGroupingIsRead() {
        assertEquals(GroupingSizes(primary = 3, secondary = 3), groupingSizesOf("10,000,000"))
    }

    @Test
    fun indianGroupingReportsBothSizes() {
        assertEquals(GroupingSizes(primary = 3, secondary = 2), groupingSizesOf("1,00,00,000"))
    }

    @Test
    fun anUngroupedProbeMeansTheLocaleDoesNotGroup() {
        assertEquals(GroupingSizes(primary = 0, secondary = 0), groupingSizesOf("10000000"))
    }
}
