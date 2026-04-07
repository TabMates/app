package de.tabmates.features.tabgroup.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import de.tabmates.core.presentation.navigation.LoggableNavKey
import de.tabmates.core.presentation.navigation.LoggedIn
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.core.presentation.util.UiText
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_home
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_home_filled
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_label

@Serializable
data object Home : LoggableNavKey(), TopLevelTab {
    override val icon: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_home)
    override val selectedIcon: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_home_filled)
    override val label: UiText = UiText.Resource(Res.string.home_label)
}

@Serializable
data object Activity : LoggableNavKey(), TopLevelTab {
    override val icon: ImageVector
        @Composable
        get() = Icons.Outlined.Notifications
    override val selectedIcon: ImageVector
        @Composable
        get() = Icons.Filled.Notifications
    override val label: UiText = UiText.Resource(Res.string.activity_label)
}

@Serializable
data object Group : LoggableNavKey(), TopLevelTab {
    override val icon: ImageVector
        @Composable
        get() = Icons.Outlined.Person
    override val selectedIcon: ImageVector
        @Composable
        get() = Icons.Filled.Person
    override val label: UiText = UiText.Resource(Res.string.group_label)
}

@Serializable
data object Profile : LoggableNavKey(), TopLevelTab {
    override val icon: ImageVector
        @Composable
        get() = Icons.Outlined.AccountCircle
    override val selectedIcon: ImageVector
        @Composable
        get() = Icons.Filled.AccountCircle
    override val label: UiText = UiText.Resource(Res.string.profile_label)
}

@Serializable
data object Settings : LoggableNavKey(), LoggedIn

@Serializable
data object AddExpense : LoggableNavKey(), LoggedIn

@Serializable
data object CreateGroup : LoggableNavKey(), LoggedIn
