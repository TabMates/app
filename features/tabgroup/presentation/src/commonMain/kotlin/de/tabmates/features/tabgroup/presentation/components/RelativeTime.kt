package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.runtime.Composable
import de.tabmates.core.presentation.util.RelativeTimeSpan
import de.tabmates.core.presentation.util.relativeTimeSpan
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_days
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_hours
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_minutes
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_updated_now
import kotlin.time.Clock
import kotlin.time.Instant

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
