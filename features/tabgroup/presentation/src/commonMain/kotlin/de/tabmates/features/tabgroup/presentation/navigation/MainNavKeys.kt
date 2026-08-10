package de.tabmates.features.tabgroup.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.edit_username_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_home
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_home_filled
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.oss_licenses_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_edit_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_title

/**
 * A screen that only makes sense while you are in [groupId].
 *
 * Marking them rather than listing them at the call site is what keeps leaving and being removed
 * from drifting apart: a route added later is scoped by construction, and nothing has to remember
 * to extend a predicate. Being removed can land on any of these, unlike leaving, which can only
 * happen from the group's settings.
 */
internal interface GroupScoped {
    val groupId: String
}

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
data object Settings : LoggableNavKey(), TopLevelTab {
    override val icon: ImageVector
        @Composable
        get() = Icons.Outlined.Settings
    override val selectedIcon: ImageVector
        @Composable
        get() = Icons.Filled.Settings
    override val label: UiText = UiText.Resource(Res.string.settings_label)
}

@Serializable
data object Profile : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.profile_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Back
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
data object UpgradeAccount : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.upgrade_account_title)
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
data class AddEntry(override val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.add_entry_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class EditEntry(
    override val groupId: String,
    val entryId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.edit_entry_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class EntryDetail(
    val entryId: String,
    override val groupId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data class SettlementDetail(
    val settlementId: String,
    override val groupId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

/**
 * Retired: settlements are edited through [EditEntry] like every other entry, now that the add form
 * handles all three kinds.
 *
 * Kept registered for one release because a persisted back stack may still hold this key, and a
 * removed polymorphic subclass fails deserialization rather than degrading. The graph redirects it.
 */
@Serializable
@Deprecated("Use EditEntry; kept only so a persisted back stack still deserializes.")
data class EditSettlement(
    override val groupId: String,
    val settlementId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.edit_entry_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

/** Read-only view of one recurring schedule, mirroring how an entry's detail screen works. */
@Serializable
data class RecurringSeriesDetail(
    override val groupId: String,
    val seriesId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

/** The add/edit form bound to a schedule instead of an entry. */
@Serializable
data class EditRecurringSeries(
    override val groupId: String,
    val seriesId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.recurring_edit_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object CreateGroup : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.create_group_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class GroupDetail(override val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithFab, GroupScoped {
    // Owns its own collapsing top bar (see GroupDetailPane), so it opts out of ScreenWithTopBar.
    override val fabAction: FabAction = FabAction.AddEntry(groupId)
}

@Serializable
data class SettleUp(override val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.settle_up_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class GroupSettings(
    override val groupId: String,
) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.group_settings_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data class GroupPeople(override val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar, GroupScoped {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.group_people_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data class JoinGroup(val token: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.join_group_title)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}
