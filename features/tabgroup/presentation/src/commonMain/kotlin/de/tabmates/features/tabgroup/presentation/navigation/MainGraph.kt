package de.tabmates.features.tabgroup.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.features.tabgroup.presentation.navigation.activity.ActivityRoot
import de.tabmates.features.tabgroup.presentation.navigation.addentry.AddEntryRoot
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.CreateGroupRoot
import de.tabmates.features.tabgroup.presentation.navigation.editsettlement.EditSettlementRoot
import de.tabmates.features.tabgroup.presentation.navigation.entrydetail.EntryDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.GroupDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupOverviewRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupsettings.GroupSettingsRoot
import de.tabmates.features.tabgroup.presentation.navigation.home.HomeRoot
import de.tabmates.features.tabgroup.presentation.navigation.joingroup.JoinGroupRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ChangeEmailRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ChangePasswordRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.DeleteAccountRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.EditUsernameRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.OssLicensesRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ProfileRoot
import de.tabmates.features.tabgroup.presentation.navigation.settlementdetail.SettlementDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.settleup.SettleUpRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_left

val mainSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Home::class)
            subclass(Activity::class)
            subclass(Group::class)
            subclass(Profile::class)
            subclass(AddEntry::class)
            subclass(EditEntry::class)
            subclass(EntryDetail::class)
            subclass(SettlementDetail::class)
            subclass(EditSettlement::class)
            subclass(CreateGroup::class)
            subclass(GroupDetail::class)
            subclass(SettleUp::class)
            subclass(GroupSettings::class)
            subclass(JoinGroup::class)
            subclass(EditUsername::class)
            subclass(ChangePassword::class)
            subclass(ChangeEmail::class)
            subclass(DeleteAccount::class)
            subclass(OssLicenses::class)
        }
    }

fun EntryProviderScope<NavKey>.mainGraph(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    appScope: CoroutineScope,
) {
    entry<Home> {
        HomeRoot(
            onGroupClick = { groupId -> backStack.add(GroupDetail(groupId)) },
            onSeeAllGroups = {
                backStack.removeAll { it is TopLevelTab }
                backStack.add(Group)
            },
        )
    }

    entry<Activity> {
        ActivityRoot(
            onGroupClick = { groupId -> backStack.add(GroupDetail(groupId)) },
        )
    }

    entry<Group> {
        GroupOverviewRoot(
            onGroupOpen = { groupId -> backStack.add(GroupDetail(groupId)) },
            onSettingsOpen = { groupId -> backStack.add(GroupSettings(groupId)) },
            onAddEntryClick = { groupId -> backStack.add(AddEntry(groupId)) },
            onEntryClick = { groupId, entryId ->
                backStack.add(EntryDetail(entryId = entryId, groupId = groupId))
            },
            snackbarHostState = snackbarHostState,
        )
    }

    entry<GroupDetail> { route ->
        val leftMessage = stringResource(Res.string.group_settings_left)
        GroupDetailRoot(
            groupId = route.groupId,
            snackbarHostState = snackbarHostState,
            onBack = { backStack.removeLastOrNull() },
            onSettingsClick = { backStack.add(GroupSettings(route.groupId)) },
            onAddEntryClick = { backStack.add(AddEntry(route.groupId)) },
            onSettleUpClick = { backStack.add(SettleUp(route.groupId)) },
            onEntryClick = { entryId ->
                backStack.add(EntryDetail(entryId = entryId, groupId = route.groupId))
            },
            onSettlementClick = { settlementId ->
                backStack.add(SettlementDetail(settlementId = settlementId, groupId = route.groupId))
            },
            onLeaveGroup = {
                backStack.removeAll {
                    (it is GroupDetail && it.groupId == route.groupId) ||
                        (it is GroupSettings && it.groupId == route.groupId)
                }
                appScope.launch { snackbarHostState.showSnackbar(leftMessage) }
            },
        )
    }

    entry<SettleUp> { route ->
        SettleUpRoot(
            groupId = route.groupId,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<Profile> {
        ProfileRoot(
            snackbarHostState = snackbarHostState,
            onEditUsername = { backStack.add(EditUsername) },
            onChangePassword = { backStack.add(ChangePassword) },
            onChangeEmail = { backStack.add(ChangeEmail) },
            onOpenOssLicenses = { backStack.add(OssLicenses) },
            onDeleteAccount = { backStack.add(DeleteAccount) },
        )
    }

    entry<OssLicenses> {
        OssLicensesRoot()
    }

    entry<EditUsername> { route ->
        EditUsernameRoot(
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeLastOrNull() },
        )
    }

    entry<ChangePassword> { route ->
        ChangePasswordRoot(
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeLastOrNull() },
        )
    }

    entry<ChangeEmail> { route ->
        ChangeEmailRoot(
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeLastOrNull() },
        )
    }

    entry<DeleteAccount> {
        // No back-stack cleanup needed: deleting clears the session and the app shell
        // (App.kt observing isLoggedIn) resets the whole stack to Welcome.
        DeleteAccountRoot(snackbarHostState = snackbarHostState)
    }

    entry<AddEntry> { route ->
        AddEntryRoot(
            groupId = route.groupId,
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeAll { it is AddEntry } },
        )
    }

    entry<EditEntry> { route ->
        AddEntryRoot(
            groupId = route.groupId,
            navKey = route,
            entryId = route.entryId,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeAll { it is EditEntry } },
        )
    }

    entry<EntryDetail> { route ->
        EntryDetailRoot(
            entryId = route.entryId,
            groupId = route.groupId,
            navKey = route,
            snackbarHostState = snackbarHostState,
            onBack = { backStack.removeLastOrNull() },
            onEdit = {
                backStack.add(EditEntry(groupId = route.groupId, entryId = route.entryId))
            },
        )
    }

    entry<SettlementDetail> { route ->
        SettlementDetailRoot(
            settlementId = route.settlementId,
            groupId = route.groupId,
            navKey = route,
            snackbarHostState = snackbarHostState,
            onBack = { backStack.removeLastOrNull() },
            onEdit = {
                backStack.add(EditSettlement(groupId = route.groupId, settlementId = route.settlementId))
            },
        )
    }

    entry<EditSettlement> { route ->
        EditSettlementRoot(
            groupId = route.groupId,
            settlementId = route.settlementId,
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeAll { it is EditSettlement } },
        )
    }

    entry<CreateGroup> {
        CreateGroupRoot(
            backStack = backStack,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<JoinGroup> { route ->
        JoinGroupRoot(
            token = route.token,
            onClose = { backStack.removeAll { it is JoinGroup } },
            onJoined = { groupId ->
                backStack.removeAll { it is JoinGroup }
                backStack.add(GroupDetail(groupId))
            },
        )
    }

    entry<GroupSettings> { route ->
        val leftMessage = stringResource(Res.string.group_settings_left)
        GroupSettingsRoot(
            groupId = route.groupId,
            onLeft = {
                backStack.removeAll { it is GroupSettings || (it is GroupDetail && it.groupId == route.groupId) }
                appScope.launch { snackbarHostState.showSnackbar(leftMessage) }
            },
            snackbarHostState = snackbarHostState,
        )
    }
}
