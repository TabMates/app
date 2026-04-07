package de.tabmates.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import de.tabmates.core.presentation.util.UiText

interface TopLevelTab : LoggedIn {
    val icon: ImageVector
        @Composable get
    val selectedIcon: ImageVector
        @Composable get
    val label: UiText
}
