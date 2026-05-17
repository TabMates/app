package de.tabmates.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import de.tabmates.core.presentation.util.UiText
import org.jetbrains.compose.resources.DrawableResource

/**
 * NavKey marker for screens that should render a top app bar with a title and a navigation action
 * (close or back). The host scaffold reads these properties to compose the top bar.
 */
interface ScreenWithTopBar {
    val topBarTitle: UiText
    val topBarAction: TopBarAction get() = TopBarAction.Back
    val trailingActions: List<TrailingTopBarAction> get() = emptyList()
}

enum class TopBarAction {
    Back,
    Close,
}

data class TrailingTopBarAction(
    val icon: DrawableResource,
    val contentDescription: UiText,
    val target: NavKey,
)
