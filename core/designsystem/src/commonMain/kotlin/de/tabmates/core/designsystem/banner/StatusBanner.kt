package de.tabmates.core.designsystem.banner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.theme.TabMatesTheme

enum class StatusBannerTone {
    /** Informational, shares the tertiary tone with the other sync indicators. */
    Info,

    /** Something needs the user to act before the app can work fully again. */
    Attention,
}

/**
 * Thin full-width strip for persistent app states (e.g. offline, expired session).
 *
 * Pass [onClick] when the state is actionable; the strip then behaves as a button, so it needs a
 * [contentDescription] that says what tapping does rather than repeating [text].
 */
@Composable
fun StatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusBannerTone = StatusBannerTone.Info,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    val (containerColor, contentColor) =
        when (tone) {
            StatusBannerTone.Info -> {
                MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            }

            StatusBannerTone.Attention -> {
                MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            }
        }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (onClick != null) {
                            Modifier
                                .clickable(
                                    onClick = onClick,
                                    onClickLabel = contentDescription,
                                    role = Role.Button,
                                )
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@PreviewThemes
@Composable
private fun StatusBannerPreview() {
    TabMatesTheme {
        StatusBanner(text = "Offline · last synced 2 h ago")
    }
}

@PreviewThemes
@Composable
private fun StatusBannerAttentionPreview() {
    TabMatesTheme {
        StatusBanner(
            text = "Session expired · sign in to sync 3 changes",
            tone = StatusBannerTone.Attention,
            onClick = {},
        )
    }
}
