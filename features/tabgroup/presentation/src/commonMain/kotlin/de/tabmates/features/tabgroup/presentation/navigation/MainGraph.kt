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
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.AddExpenseRoot
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.CreateGroupRoot
import de.tabmates.features.tabgroup.presentation.navigation.expensedetail.ExpenseDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.GroupDetailRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupOverviewRoot
import de.tabmates.features.tabgroup.presentation.navigation.groupsettings.GroupSettingsRoot
import de.tabmates.features.tabgroup.presentation.navigation.home.HomeRoot
import de.tabmates.features.tabgroup.presentation.navigation.joingroup.JoinGroupRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ChangePasswordRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.EditUsernameRoot
import de.tabmates.features.tabgroup.presentation.navigation.profile.ProfileRoot
import de.tabmates.features.tabgroup.presentation.navigation.settleup.SettleUpRoot
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
            subclass(EditExpense::class)
            subclass(ExpenseDetail::class)
            subclass(CreateGroup::class)
            subclass(GroupDetail::class)
            subclass(SettleUp::class)
            subclass(GroupSettings::class)
            subclass(JoinGroup::class)
            subclass(EditUsername::class)
            subclass(ChangePassword::class)
        }
    }

fun EntryProviderScope<NavKey>.mainGraph(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
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
        PlaceholderScreen("Activity")
    }

    entry<Group> {
        GroupOverviewRoot(
            onGroupOpen = { groupId -> backStack.add(GroupDetail(groupId)) },
            onSettingsOpen = { groupId -> backStack.add(GroupSettings(groupId)) },
            onAddExpenseClick = { groupId -> backStack.add(AddExpense(groupId)) },
            onExpenseClick = { groupId, expenseId ->
                backStack.add(ExpenseDetail(expenseId = expenseId, groupId = groupId))
            },
            snackbarHostState = snackbarHostState,
        )
    }

    entry<GroupDetail> { route ->
        GroupDetailRoot(
            groupId = route.groupId,
            snackbarHostState = snackbarHostState,
            onSettingsClick = { backStack.add(GroupSettings(route.groupId)) },
            onAddExpenseClick = { backStack.add(AddExpense(route.groupId)) },
            onSettleUpClick = { backStack.add(SettleUp(route.groupId)) },
            onExpenseClick = { expenseId ->
                backStack.add(ExpenseDetail(expenseId = expenseId, groupId = route.groupId))
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
        )
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

    entry<Settings> {
        PlaceholderScreen("Settings")
    }

    entry<AddExpense> { route ->
        AddExpenseRoot(
            groupId = route.groupId,
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeAll { it is AddExpense } },
        )
    }

    entry<EditExpense> { route ->
        AddExpenseRoot(
            groupId = route.groupId,
            navKey = route,
            expenseId = route.expenseId,
            snackbarHostState = snackbarHostState,
            onSaved = { backStack.removeAll { it is EditExpense } },
        )
    }

    entry<ExpenseDetail> { route ->
        ExpenseDetailRoot(
            expenseId = route.expenseId,
            groupId = route.groupId,
            navKey = route,
            snackbarHostState = snackbarHostState,
            onBack = { backStack.removeLastOrNull() },
            onEdit = {
                backStack.add(EditExpense(groupId = route.groupId, expenseId = route.expenseId))
            },
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
