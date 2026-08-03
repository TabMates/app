package de.tabmates.core.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberFormatterTest {
    @Test
    fun symbolLeadsAndGroupsPerLocale() {
        assertEquals("€1,234.56", formatMoney("€", 1234.56, 2, EN_US))
        assertEquals("1.234,56" + NBSP + "€", formatMoney("€", 1234.56, 2, DE_DE))
        assertEquals("€" + NBSP + "1'234.56", formatMoney("€", 1234.56, 2, DE_CH))
        assertEquals("1" + NARROW_NBSP + "234,56" + NBSP + "€", formatMoney("€", 1234.56, 2, FR_FR))
    }

    @Test
    fun currencyDecimalsComeFromTheCurrencyNotTheLocale() {
        assertEquals("1.235" + NBSP + "¥", formatMoney("¥", 1234.6, 0, DE_DE))
        assertEquals("¥1,235", formatMoney("¥", 1234.6, 0, EN_US))
    }

    @Test
    fun secondaryGroupingSizeIsHonoured() {
        assertEquals("₹1,00,00,000.00", formatMoney("₹", 10_000_000.0, 2, HI_IN))
        assertEquals("₹10,000,000.00", formatMoney("₹", 10_000_000.0, 2, EN_US))
    }

    @Test
    fun localeWithoutGroupingGetsNoSeparators() {
        assertEquals("$1234.50", formatMoney("$", 1234.5, 2, NO_GROUPING))
    }

    @Test
    fun negativeUsesTheLocaleMinusInFront() {
        assertEquals("-€5.00", formatMoney("€", -5.0, 2, EN_US))
        assertEquals("-5,00" + NBSP + "€", formatMoney("€", -5.0, 2, DE_DE))
    }

    @Test
    fun amountThatRoundsToZeroIsNotSigned() {
        assertEquals("$0.00", formatMoney("$", -0.001, 2, EN_US))
    }

    @Test
    fun emptySymbolIsOmittedEntirely() {
        assertEquals("1.234,56", formatMoney("", 1234.56, 2, DE_DE))
    }

    @Test
    fun unsignedDropsTheSign() {
        assertEquals("€5.00", formatMoneyUnsigned("€", -5.0, 2, EN_US))
    }

    @Test
    fun explicitSignLeadsRegardlessOfSymbolPosition() {
        assertEquals("+€5.00", formatSignedMoney("€", 5.0, 2, EN_US, AmountSign.Positive))
        assertEquals("−5,00" + NBSP + "€", formatSignedMoney("€", 5.0, 2, DE_DE, AmountSign.Negative))
        assertEquals("€5.00", formatSignedMoney("€", 5.0, 2, EN_US, AmountSign.None))
    }

    @Test
    fun percentCarriesTheLocaleSymbolAndSpacing() {
        assertEquals("50%", formatPercent(50.0, EN_US))
        assertEquals("50" + NBSP + "%", formatPercent(50.0, DE_DE))
        assertEquals("%50", formatPercent(50.0, TR_TR))
    }

    @Test
    fun percentKeepsMeaningfulDecimalsAndDropsTrailingZeros() {
        assertEquals("33.33%", formatPercent(33.333, EN_US))
        assertEquals("12,5" + NBSP + "%", formatPercent(12.5, DE_DE))
        assertEquals("-12.5%", formatPercent(-12.5, EN_US))
    }

    @Test
    fun rateTrimsTrailingZerosAndUsesLocaleSeparator() {
        assertEquals("0.92", formatRate(0.92, EN_US))
        assertEquals("0,92", formatRate(0.92, DE_DE))
        assertEquals("1", formatRate(1.0, EN_US))
    }

    @Test
    fun rateKeepsFourSignificantDigitsAcrossMagnitudes() {
        assertEquals("149.3", formatRate(149.32, EN_US))
        assertEquals("1.087", formatRate(1.08654, EN_US))
        assertEquals("0.00004", formatRate(0.00004, EN_US))
        assertEquals("1", formatRate(0.99999, EN_US))
    }

    @Test
    fun rateReturnsNullWhenItCannotBeShown() {
        assertNull(formatRate(0.0, EN_US))
        assertNull(formatRate(-1.5, EN_US))
        assertNull(formatRate(Double.NaN, EN_US))
        assertNull(formatRate(Double.POSITIVE_INFINITY, EN_US))
    }

    @Test
    fun inputFormattingUsesTheLocaleSeparatorButNeverGroups() {
        assertEquals("1234,50", formatAmountForInput(1234.5, 2, DE_DE))
        assertEquals("1234.50", formatAmountForInput(1234.5, 2, EN_US))
        assertEquals("10.00", formatAmountForInput(10.0, 2, EN_US))
        assertEquals("1235", formatAmountForInput(1234.6, 0, EN_US))
    }

    @Test
    fun largeAmountsStillGroup() {
        assertEquals("$1,234,567,890.12", formatMoney("$", 1_234_567_890.12, 2, EN_US))
    }

    @Test
    fun epsilonMatchesTheSmallestVisibleAmount() {
        assertEquals(0.005, amountEpsilon(2))
        assertEquals(0.5, amountEpsilon(0))
    }

    @Test
    fun roundingMatchesWhatIsDisplayed() {
        assertEquals(12.35, roundToDecimals(12.3456, 2))
        assertEquals(12.0, roundToDecimals(12.3456, 0))
    }

    @Test
    fun tiesGoUpRatherThanToTheEvenCent() {
        // kotlin.math.round would give 0.12 / 0.62 here, because it breaks ties towards even.
        assertEquals("0.13", formatMoney("", 0.125, 2, EN_US))
        assertEquals("0.63", formatMoney("", 0.625, 2, EN_US))
        assertEquals("0.38", formatMoney("", 0.375, 2, EN_US))
        assertEquals("3", formatMoney("", 2.5, 0, EN_US))
    }

    @Test
    fun tiesRoundAwayFromZeroOnBothSides() {
        assertEquals(0.13, roundToDecimals(0.125, 2))
        assertEquals(-0.13, roundToDecimals(-0.125, 2))
        assertEquals("-0.13", formatMoney("", -0.125, 2, EN_US))
    }

    @Test
    fun aValueOnlyLookingLikeATieIsNotOne() {
        // 1.005 * 100 is 100.49999999999999 as a Double, so it is genuinely below the tie.
        // Half-up cannot rescue this; only decimal arithmetic could.
        assertEquals("1.00", formatMoney("", 1.005, 2, EN_US))
    }
}
