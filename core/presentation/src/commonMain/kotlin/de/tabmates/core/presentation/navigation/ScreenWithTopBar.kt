package de.tabmates.core.presentation.navigation

import de.tabmates.core.presentation.util.UiText

/**
 * NavKey marker for screens that should render a top app bar with a title and a navigation action
 * (close or back). The host scaffold reads these properties to compose the top bar.
 */
interface ScreenWithTopBar {
    val topBarTitle: UiText
    val topBarAction: TopBarAction get() = TopBarAction.Back
}

enum class TopBarAction {
    Back,
    Close,
}
