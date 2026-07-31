package de.tabmates.composeapp.promo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.banner.StatusBanner
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.app_promo_banner_cd
import tabmatesapp.composeapp.generated.resources.app_promo_banner_dismiss
import tabmatesapp.composeapp.generated.resources.app_promo_banner_text
import tabmatesapp.composeapp.generated.resources.ic_close

@Composable
fun AppPromoBannerRoot(
    modifier: Modifier = Modifier,
    viewModel: AppPromoBannerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppPromoBanner(
        state = state,
        onOpenClick = { openAppPromoTarget(androidAppPromoIntentUrl()) },
        onDismissClick = viewModel::onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun AppPromoBanner(
    state: AppPromoBannerState,
    onOpenClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = state.isVisible, modifier = modifier) {
        StatusBanner(
            text = stringResource(Res.string.app_promo_banner_text),
            onClick = onOpenClick,
            contentDescription = stringResource(Res.string.app_promo_banner_cd),
            trailingContent = {
                IconButton(onClick = onDismissClick) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.app_promo_banner_dismiss),
                    )
                }
            },
        )
    }
}
