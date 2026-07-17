package de.tabmates.core.presentation.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

sealed interface RelativeTimeSpan {
    data object JustNow : RelativeTimeSpan

    data class Minutes(val value: Int) : RelativeTimeSpan

    data class Hours(val value: Int) : RelativeTimeSpan

    data class Days(val value: Int) : RelativeTimeSpan
}

/** Buckets the time elapsed since [from]; negative (clock-skewed) values clamp to [RelativeTimeSpan.JustNow]. */
fun relativeTimeSpan(
    from: Instant,
    now: Instant,
): RelativeTimeSpan {
    val elapsed = (now - from).coerceAtLeast(Duration.ZERO)
    return when {
        elapsed < 1.minutes -> RelativeTimeSpan.JustNow
        elapsed < 1.hours -> RelativeTimeSpan.Minutes(elapsed.inWholeMinutes.toInt())
        elapsed < 1.days -> RelativeTimeSpan.Hours(elapsed.inWholeHours.toInt())
        else -> RelativeTimeSpan.Days(elapsed.inWholeDays.toInt())
    }
}
