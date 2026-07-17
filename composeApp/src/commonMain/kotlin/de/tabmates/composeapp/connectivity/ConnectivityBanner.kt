package de.tabmates.composeapp.connectivity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.banner.StatusBanner
import de.tabmates.core.presentation.util.RelativeTimeSpan
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.connectivity_banner_offline_days
import tabmatesapp.composeapp.generated.resources.connectivity_banner_offline_hours
import tabmatesapp.composeapp.generated.resources.connectivity_banner_offline_minutes
import tabmatesapp.composeapp.generated.resources.connectivity_banner_offline_now
import tabmatesapp.composeapp.generated.resources.connectivity_banner_offline_unsynced

@Composable
fun ConnectivityBannerRoot(
    modifier: Modifier = Modifier,
    viewModel: ConnectivityBannerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConnectivityBanner(
        state = state,
        modifier = modifier,
    )
}

@Composable
private fun ConnectivityBanner(
    state: ConnectivityBannerState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = state.isVisible, modifier = modifier) {
        StatusBanner(text = offlineBannerText(state.lastSyncedSpan))
    }
}

@Composable
private fun offlineBannerText(span: RelativeTimeSpan?): String =
    when (span) {
        null -> stringResource(Res.string.connectivity_banner_offline_unsynced)
        RelativeTimeSpan.JustNow -> stringResource(Res.string.connectivity_banner_offline_now)
        is RelativeTimeSpan.Minutes -> stringResource(Res.string.connectivity_banner_offline_minutes, span.value)
        is RelativeTimeSpan.Hours -> stringResource(Res.string.connectivity_banner_offline_hours, span.value)
        is RelativeTimeSpan.Days -> stringResource(Res.string.connectivity_banner_offline_days, span.value)
    }
