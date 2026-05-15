package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.IconCatalog
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.abs

/**
 * Picks a stable [iconKey] + [colorKey] pair for a group whose icon isn't persisted yet,
 * derived deterministically from the group's id.
 */
fun deriveGroupAvatarKey(groupId: String): Pair<String, String> {
    val icons = IconCatalog.iconsByCategory.values.flatten()
    val colors = IconCatalog.colors
    val hash = abs(groupId.hashCode())
    val iconKey = icons.getOrNull(hash % icons.size)?.key ?: IconCatalog.defaultIconKey
    val colorKey =
        colors.getOrNull((hash / icons.size.coerceAtLeast(1)) % colors.size)?.key
            ?: IconCatalog.defaultColorKey
    return iconKey to colorKey
}

@Composable
fun GroupAvatar(
    iconKey: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = size / 2,
) {
    val color = IconCatalog.colorOption(colorKey)
    val icon = IconCatalog.iconOption(iconKey) ?: return
    Box(
        modifier =
            modifier
                .size(size)
                .background(color.color, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon.drawable),
            contentDescription = null,
            tint = color.onColor,
            modifier = Modifier.size(iconSize),
        )
    }
}
