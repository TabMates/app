package de.tabmates.composeapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.navigation.LoggedIn
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.features.authentication.presentation.navigation.authGraph
import de.tabmates.features.authentication.presentation.navigation.Welcome
import de.tabmates.features.authentication.presentation.navigation.authSerializersModule
import de.tabmates.features.tabgroup.presentation.navigation.Activity
import de.tabmates.features.tabgroup.presentation.navigation.Group
import de.tabmates.features.tabgroup.presentation.navigation.Home
import de.tabmates.features.tabgroup.presentation.navigation.Profile
import de.tabmates.features.tabgroup.presentation.navigation.mainGraph
import de.tabmates.features.tabgroup.presentation.navigation.mainSerializersModule
import kotlinx.serialization.modules.plus

private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = authSerializersModule + mainSerializersModule
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    TabMatesTheme {
        val backStack = rememberNavBackStack(configuration = savedStateConfiguration, Welcome)
        val currentKey = backStack.lastOrNull()

        val topLevelTabs = remember { listOf(Home, Activity, Group, Profile) }

        if (currentKey is LoggedIn) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    topLevelTabs.forEach { tab ->
                        val selected = currentKey == tab
                        item(
                            selected = selected,
                            onClick = {
                                backStack.removeAll { it is TopLevelTab }
                                backStack.add(tab)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label.asString(),
                                )
                            },
                            label = { Text(tab.label.asString()) },
                        )
                    }
                },
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        mainGraph(backStack)
                    },
                )
            }
        } else {
            Scaffold {
                NavDisplay(
                    modifier = Modifier.fillMaxSize().padding(it),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        authGraph(
                            backStack = backStack,
                            onGuestClick = {
                                backStack.clear()
                                backStack.add(Home)
                            },
                        )
                    }
                )
            }
        }
    }
}
