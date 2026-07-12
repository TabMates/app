package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_days
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_hours
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_minutes
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_now
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal sealed interface RelativeTimeSpan {
    data object JustNow : RelativeTimeSpan

    data class Minutes(val value: Int) : RelativeTimeSpan

    data class Hours(val value: Int) : RelativeTimeSpan

    data class Days(val value: Int) : RelativeTimeSpan
}

/** Buckets the time elapsed since [from]; negative (clock-skewed) values clamp to [RelativeTimeSpan.JustNow]. */
internal fun relativeTimeSpan(
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

/** "Updated 2 h ago" style label for an exchange-rate timestamp. */
@Composable
internal fun rateUpdatedLabel(
    lastUpdatedAt: Instant,
    now: Instant = Clock.System.now(),
): String =
    when (val span = relativeTimeSpan(from = lastUpdatedAt, now = now)) {
        RelativeTimeSpan.JustNow -> stringResource(Res.string.currency_rate_updated_now)
        is RelativeTimeSpan.Minutes -> stringResource(Res.string.currency_rate_updated_minutes, span.value)
        is RelativeTimeSpan.Hours -> stringResource(Res.string.currency_rate_updated_hours, span.value)
        is RelativeTimeSpan.Days -> stringResource(Res.string.currency_rate_updated_days, span.value)
    }
