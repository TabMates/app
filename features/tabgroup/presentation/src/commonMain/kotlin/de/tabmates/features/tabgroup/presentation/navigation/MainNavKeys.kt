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
import de.tabmates.core.presentation.navigation.FabAction
import de.tabmates.core.presentation.navigation.LoggableNavKey
import de.tabmates.core.presentation.navigation.LoggedIn
import de.tabmates.core.presentation.navigation.ScreenWithFab
import de.tabmates.core.presentation.navigation.ScreenWithTopBar
import de.tabmates.core.presentation.navigation.TopBarAction
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.core.presentation.util.UiText
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_password_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.edit_entry_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.edit_settlement_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.edit_username_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_home
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_home_filled
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.oss_licenses_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_title

@Serializable
data object Home : LoggableNavKey(), TopLevelTab, ScreenWithFab {
    override val icon: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_home)
    override val selectedIcon: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_home_filled)
    override val label: UiText = UiText.Resource(Res.string.home_label)
    override val fabAction: FabAction = FabAction.CreateGroup
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
data object Group : LoggableNavKey(), TopLevelTab, ScreenWithFab {
    override val icon: ImageVector
        @Composable
        get() = Icons.Outlined.Person
    override val selectedIcon: ImageVector
        @Composable
        get() = Icons.Filled.Person
    override val label: UiText = UiText.Resource(Res.string.group_label)
    override val fabAction: FabAction = FabAction.CreateGroup
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
data object EditUsername : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.edit_username_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object ChangePassword : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.change_password_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object ChangeEmail : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.change_email_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object DeleteAccount : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.delete_account_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object OssLicenses : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.oss_licenses_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data class AddEntry(val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.add_entry_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class EditEntry(val groupId: String, val entryId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.edit_entry_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class EntryDetail(
    val entryId: String,
    val groupId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data class SettlementDetail(
    val settlementId: String,
    val groupId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data class EditSettlement(
    val groupId: String,
    val settlementId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.edit_settlement_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object CreateGroup : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.create_group_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class GroupDetail(val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, ScreenWithFab {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
    override val fabAction: FabAction = FabAction.AddEntry(groupId)
}

@Serializable
data class SettleUp(val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.settle_up_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class GroupSettings(val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.group_settings_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data class JoinGroup(val token: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.join_group_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}
