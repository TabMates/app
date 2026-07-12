package de.tabmates.features.tabgroup.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RateFormatTest {
    @Test
    fun typicalRateTrimsTrailingZeros() {
        assertEquals("0.92", formatRate(0.92))
    }

    @Test
    fun largeRateRoundsToSignificantDigits() {
        assertEquals("149.3", formatRate(149.32))
    }

    @Test
    fun verySmallRateKeepsPrecision() {
        assertEquals("0.00004", formatRate(0.00004))
    }

    @Test
    fun wholeNumberDropsDecimalPoint() {
        assertEquals("1", formatRate(1.0))
    }

    @Test
    fun midMagnitudeRateRoundsToFourSignificantDigits() {
        assertEquals("1.087", formatRate(1.08654))
    }

    @Test
    fun roundingUpAcrossMagnitudeBoundary() {
        assertEquals("1", formatRate(0.99999))
    }

    @Test
    fun zeroNegativeAndNonFiniteReturnNull() {
        assertNull(formatRate(0.0))
        assertNull(formatRate(-1.5))
        assertNull(formatRate(Double.NaN))
        assertNull(formatRate(Double.POSITIVE_INFINITY))
    }
}
