package de.tabmates.features.tabgroup.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RelativeTimeTest {
    private val now = Instant.fromEpochMilliseconds(1_752_000_000_000)

    @Test
    fun underOneMinuteIsJustNow() {
        assertEquals(RelativeTimeSpan.JustNow, relativeTimeSpan(from = now, now = now))
        assertEquals(RelativeTimeSpan.JustNow, relativeTimeSpan(from = now - 59.seconds, now = now))
    }

    @Test
    fun minutesBetweenOneMinuteAndOneHour() {
        assertEquals(RelativeTimeSpan.Minutes(1), relativeTimeSpan(from = now - 1.minutes, now = now))
        val almostAnHour = relativeTimeSpan(from = now - 59.minutes - 30.seconds, now = now)
        assertEquals(RelativeTimeSpan.Minutes(59), almostAnHour)
    }

    @Test
    fun hoursBetweenOneHourAndOneDay() {
        assertEquals(RelativeTimeSpan.Hours(1), relativeTimeSpan(from = now - 1.hours, now = now))
        assertEquals(RelativeTimeSpan.Hours(23), relativeTimeSpan(from = now - 23.hours - 59.minutes, now = now))
    }

    @Test
    fun daysFromOneDayOnwards() {
        assertEquals(RelativeTimeSpan.Days(1), relativeTimeSpan(from = now - 1.days, now = now))
        assertEquals(RelativeTimeSpan.Days(400), relativeTimeSpan(from = now - 400.days, now = now))
    }

    @Test
    fun futureTimestampClampsToJustNow() {
        assertEquals(RelativeTimeSpan.JustNow, relativeTimeSpan(from = now + 5.minutes, now = now))
    }
}
