package de.tabmates.core.designsystem.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.extended

/**
 * Full-screen outcome of something the user just finished, or that just failed — a confirmed email,
 * a redeemed invite, an empty list.
 *
 * Scrolls instead of centring rigidly: a badge, a headline, two paragraphs and a button stop
 * fitting in landscape and on short windows, and clipped copy on a screen the user cannot leave any
 * other way is worse than a scrollbar.
 */
@Composable
fun ResultLayout(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    supportingText: String? = null,
    badge: (@Composable () -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (badge != null) {
            badge()
            VerticalSpacer(24.dp)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            VerticalSpacer(12.dp)
            Text(
                modifier = Modifier.widthIn(max = 400.dp),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (supportingText != null) {
            VerticalSpacer(12.dp)
            Text(
                modifier = Modifier.widthIn(max = 400.dp),
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        VerticalSpacer(32.dp)
        actions()
    }
}

enum class ResultTone {
    Positive,
    Error,
}

/**
 * The circular icon that heads a [ResultLayout].
 *
 * The icon is a parameter rather than something the tone picks: artwork belongs to the features, so
 * the design system owns only the shape and the colour pairing.
 */
@Composable
fun ResultBadge(
    icon: ImageVector,
    tone: ResultTone,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val containerColor =
        when (tone) {
            ResultTone.Positive -> MaterialTheme.colorScheme.extended.positiveContainer
            ResultTone.Error -> MaterialTheme.colorScheme.errorContainer
        }
    val contentColor =
        when (tone) {
            ResultTone.Positive -> MaterialTheme.colorScheme.extended.onPositiveContainer
            ResultTone.Error -> MaterialTheme.colorScheme.onErrorContainer
        }

    // Keyed on the icon so the badge pops once when the outcome lands, and again only if the
    // outcome itself changes.
    val scale = remember { Animatable(INITIAL_BADGE_SCALE) }
    LaunchedEffect(icon) {
        scale.snapTo(INITIAL_BADGE_SCALE)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        )
    }

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }.size(88.dp)
                .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(40.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
        )
    }
}

private const val INITIAL_BADGE_SCALE = 0.8f

@PreviewThemes
@Composable
private fun ResultLayoutPreview() {
    TabMatesTheme {
        Surface {
            ResultLayout(
                title = "Email confirmed",
                description = "Your email address is confirmed. Sign in to start using TabMates.",
                badge = {
                    ResultBadge(icon = Icons.Default.Check, tone = ResultTone.Positive)
                },
                actions = {
                    TabMatesButton(
                        modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
                        text = "Login",
                        onClick = {},
                    )
                },
            )
        }
    }
}
