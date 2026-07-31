package de.tabmates.composeapp.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.tabmates.core.designsystem.banner.StatusBanner
import de.tabmates.core.designsystem.banner.StatusBannerTone
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.session_banner_expired
import tabmatesapp.composeapp.generated.resources.session_banner_expired_cd

/**
 * Shown in place of the connectivity banner while the session is expired. The socket is
 * deliberately down in that state, so the offline banner would otherwise sit alongside this one
 * blaming the network for what is actually an auth problem.
 *
 * Deliberately says nothing about *how much* is unsynced — that number belongs on the re-auth
 * screen, next to the choice it actually informs.
 */
@Composable
fun SessionExpiredBanner(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBanner(
        text = stringResource(Res.string.session_banner_expired),
        modifier = modifier,
        tone = StatusBannerTone.Attention,
        onClick = onSignInClick,
        contentDescription = stringResource(Res.string.session_banner_expired_cd),
    )
}
