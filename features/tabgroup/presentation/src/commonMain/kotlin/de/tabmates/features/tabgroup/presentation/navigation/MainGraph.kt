package de.tabmates.features.tabgroup.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

fun EntryProviderScope<NavKey>.mainGraph(backStack: NavBackStack<NavKey>) {
    entry<Home> {
        PlaceholderScreen("Home")
    }

    entry<Activity> {
        PlaceholderScreen("Activity")
    }

    entry<Group> {
        PlaceholderScreen("Group")
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
        PlaceholderScreen("Create Group")
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
