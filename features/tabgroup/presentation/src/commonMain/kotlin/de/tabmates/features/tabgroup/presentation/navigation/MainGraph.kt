package de.tabmates.features.tabgroup.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.CreateGroupRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.GroupDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupOverviewRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupsettings.GroupSettingsRoot
import de.tabmates.features.tabgroup.presentation.navigation.home.HomeRoot
import de.tabmates.features.tabgroup.presentation.navigation.joingroup.JoinGroupRoot
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val mainSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Home::class)
            subclass(Activity::class)
            subclass(Group::class)
            subclass(Profile::class)
            subclass(Settings::class)
            subclass(AddExpense::class)
            subclass(CreateGroup::class)
            subclass(GroupDetail::class)
            subclass(GroupSettings::class)
            subclass(JoinGroup::class)
        }
    }

fun EntryProviderScope<NavKey>.mainGraph(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
) {
    entry<Home> {
        HomeRoot()
    }

    entry<Activity> {
        PlaceholderScreen("Activity")
    }

    entry<Group> {
        GroupOverviewRoot(
            onGroupOpen = { groupId -> backStack.add(GroupDetail(groupId)) },
            onSettingsOpen = { groupId -> backStack.add(GroupSettings(groupId)) },
            snackbarHostState = snackbarHostState,
        )
    }

    entry<GroupDetail> { route ->
        GroupDetailRoot(
            groupId = route.groupId,
            snackbarHostState = snackbarHostState,
            onSettingsClick = { backStack.add(GroupSettings(route.groupId)) },
        )
    }

    entry<Profile> {
        PlaceholderScreen("Profile")
    }

    entry<Settings> {
        PlaceholderScreen("Settings")
    }

    entry<AddExpense> {
        PlaceholderScreen("Add Expense")
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
        GroupSettingsRoot(
            groupId = route.groupId,
            onLeft = {
                backStack.removeAll { it is GroupSettings || (it is GroupDetail && it.groupId == route.groupId) }
            },
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title)
    }
}
