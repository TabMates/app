package de.tabmates.features.tabgroup.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.presentation.navigation.PaneRole
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.domain.group.GroupRemovalNotifier
import de.tabmates.features.tabgroup.presentation.navigation.activity.ActivityRoot
import de.tabmates.features.tabgroup.presentation.navigation.addentry.AddEntryRoot
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.CreateGroupRoot
import de.tabmates.features.tabgroup.presentation.navigation.entrydetail.EntryDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.GroupDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupOverviewRoot
import de.tabmates.features.tabgroup.presentation.navigation.grouppeople.GroupPeopleRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupsettings.GroupSettingsRoot
import de.tabmates.features.tabgroup.presentation.navigation.home.HomeRoot
import de.tabmates.features.tabgroup.presentation.navigation.joingroup.JoinGroupRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ChangeEmailRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ChangePasswordRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.DeleteAccountRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.EditUsernameRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ProfileRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.UpgradeAccountRoot
import de.tabmates.features.tabgroup.presentation.navigation.recurringdetail.RecurringSeriesDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.settings.OssLicensesRoot
import de.tabmates.features.tabgroup.presentation.navigation.settings.SettingsRoot
import de.tabmates.features.tabgroup.presentation.navigation.settlementdetail.SettlementDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.settleup.SettleUpRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_removed_snackbar
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_removed_snackbar_unnamed
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_left

val mainSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Home::class)
            subclass(Activity::class)
            subclass(Group::class)
            subclass(Settings::class)
            subclass(Profile::class)
            subclass(AddEntry::class)
            subclass(EditEntry::class)
            subclass(EntryDetail::class)
            subclass(SettlementDetail::class)
            @Suppress("DEPRECATION")
            subclass(EditSettlement::class)
            subclass(RecurringSeriesDetail::class)
            subclass(EditRecurringSeries::class)
            subclass(CreateGroup::class)
            subclass(GroupDetail::class)
            subclass(SettleUp::class)
            subclass(GroupSettings::class)
            subclass(GroupPeople::class)
            subclass(JoinGroup::class)
            subclass(EditUsername::class)
            subclass(ChangePassword::class)
            subclass(ChangeEmail::class)
            subclass(UpgradeAccount::class)
            subclass(DeleteAccount::class)
            subclass(OssLicenses::class)
        }
    }

/**
 * Drops every screen belonging to [groupId], leaving whatever came before it on the stack.
 *
 * Scoped rather than "pop to the group list": on a wide window another group's pages can sit on the
 * same stack, and they are still valid. Shared by leaving and by being removed — the second can
 * land while any of these screens is open, so the set has to be the whole of [GroupScoped].
 */
fun NavBackStack<NavKey>.removeGroupScopedEntries(groupId: String) {
    removeAll { it is GroupScoped && it.groupId == groupId }
}

/**
 * Reacts to *this* user being removed from a group by someone else.
 *
 * Belongs to the shell rather than to any screen: by the time the frame arrives the group is gone
 * locally, so whichever of its screens is open has nothing left to render. Call it once, above both
 * adaptive layouts — twice would pop correctly but say so twice.
 */
@Composable
fun ObserveGroupRemovals(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    notifier: GroupRemovalNotifier = koinInject(),
) {
    ObserveAsEvents(notifier.removals) { removal ->
        backStack.removeGroupScopedEntries(removal.groupId)
        // The title comes from the local mirror, which a client that never saw the group may not
        // have had — say the plain thing rather than name an empty group.
        val message =
            removal.title
                ?.let { getString(Res.string.group_removed_snackbar, it) }
                ?: getString(Res.string.group_removed_snackbar_unnamed)
        snackbarHostState.showSnackbar(message)
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
            onEntryClick = { groupId, entryId ->
                backStack.add(EntryDetail(entryId = entryId, groupId = groupId))
            },
            onSettlementClick = { groupId, settlementId ->
                backStack.add(SettlementDetail(settlementId = settlementId, groupId = groupId))
            },
        )
    }

    // The group list and whatever sits on top of it form the two panes on wide windows; see
    // GroupTwoPaneSceneStrategy, which reads these roles back off the entries.
    entry<Group>(metadata = PaneRole.list) {
        // The back stack is the selection: no parallel flag to drift out of sync, and system back
        // clears the detail pane on its own.
        val selectedGroupId = backStack.filterIsInstance<GroupDetail>().lastOrNull()?.groupId
        GroupOverviewRoot(
            selectedGroupId = selectedGroupId,
            onGroupOpen = { groupId ->
                // Replace rather than stack: picking another group swaps the pane, it does not
                // deepen the history. Settings and People go too, or the previous group's pages
                // would sit under the newly selected group in the detail pane.
                backStack.removeAll {
                    it is GroupDetail || it is SettleUp || it is GroupSettings || it is GroupPeople
                }
                backStack.add(GroupDetail(groupId))
            },
        )
    }

    entry<GroupDetail>(metadata = PaneRole.detail) { route ->
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
            onRecurringSeriesClick = { seriesId ->
                backStack.add(RecurringSeriesDetail(groupId = route.groupId, seriesId = seriesId))
            },
        )
    }

    entry<SettleUp>(metadata = PaneRole.detail) { route ->
        SettleUpRoot(
            groupId = route.groupId,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<Settings> {
        SettingsRoot(
            onProfileClick = { backStack.add(Profile) },
            onUpgradeAccount = { backStack.add(UpgradeAccount) },
            onOpenOssLicenses = { backStack.add(OssLicenses) },
        )
    }

    entry<Profile> {
        ProfileRoot(
            onEditUsername = { backStack.add(EditUsername) },
            onChangeEmail = { backStack.add(ChangeEmail) },
            onChangePassword = { backStack.add(ChangePassword) },
            onUpgradeAccount = { backStack.add(UpgradeAccount) },
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

    entry<UpgradeAccount> {
        UpgradeAccountRoot(
            snackbarHostState = snackbarHostState,
            onCompleted = { backStack.removeLastOrNull() },
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
                backStack.add(EditEntry(groupId = route.groupId, entryId = route.settlementId))
            },
        )
    }

    // Retired. Only reachable from a back stack persisted by an older build, so it swaps itself
    // for the entry form rather than rendering — removing the key outright would fail to
    // deserialize and take the whole restored stack with it.
    @Suppress("DEPRECATION")
    entry<EditSettlement> { route ->
        LaunchedEffect(route) {
            backStack.removeAll { it is EditSettlement }
            backStack.add(EditEntry(groupId = route.groupId, entryId = route.settlementId))
        }
    }

    entry<RecurringSeriesDetail> { route ->
        RecurringSeriesDetailRoot(
            groupId = route.groupId,
            seriesId = route.seriesId,
            snackbarHostState = snackbarHostState,
            onBack = { backStack.removeLastOrNull() },
            onEdit = { seriesId ->
                backStack.add(EditRecurringSeries(groupId = route.groupId, seriesId = seriesId))
            },
        )
    }

    entry<EditRecurringSeries> { route ->
        AddEntryRoot(
            groupId = route.groupId,
            navKey = route,
            seriesId = route.seriesId,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeAll { it is EditRecurringSeries } },
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

    entry<GroupSettings>(metadata = PaneRole.detail) { route ->
        val leftMessage = stringResource(Res.string.group_settings_left)
        GroupSettingsRoot(
            groupId = route.groupId,
            onPeopleClick = { backStack.add(GroupPeople(route.groupId)) },
            onLeft = {
                backStack.removeGroupScopedEntries(route.groupId)
                appScope.launch { snackbarHostState.showSnackbar(leftMessage) }
            },
            snackbarHostState = snackbarHostState,
        )
    }

    entry<GroupPeople>(metadata = PaneRole.detail) { route ->
        GroupPeopleRoot(
            groupId = route.groupId,
            snackbarHostState = snackbarHostState,
        )
    }
}
