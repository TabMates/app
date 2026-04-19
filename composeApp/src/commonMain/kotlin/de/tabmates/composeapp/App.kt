package de.tabmates.composeapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import de.tabmates.composeapp.deeplink.DeepLinkHandler
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.ic_arrow_back
import de.tabmates.composeapp.deeplink.navDeepLink
import de.tabmates.composeapp.deeplink.resolveDeepLink
import de.tabmates.composeapp.di.appModule
import de.tabmates.core.data.di.coreDataModule
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.navigation.LoggedIn
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.features.authentication.data.di.authenticationDataModule
import de.tabmates.features.authentication.presentation.di.authPresentationModule
import de.tabmates.features.authentication.presentation.navigation.EmailVerification
import de.tabmates.features.authentication.presentation.navigation.authGraph
import de.tabmates.features.authentication.presentation.navigation.ResetPassword
import de.tabmates.features.authentication.presentation.navigation.Welcome
import de.tabmates.features.authentication.presentation.navigation.authSerializersModule
import de.tabmates.features.tabgroup.data.di.tabgroupDataModule
import de.tabmates.features.tabgroup.presentation.navigation.Activity
import de.tabmates.features.tabgroup.presentation.navigation.Group
import de.tabmates.features.tabgroup.presentation.navigation.Home
import de.tabmates.features.tabgroup.presentation.navigation.Profile
import de.tabmates.features.tabgroup.presentation.navigation.di.tabgroupPresentationModule
import de.tabmates.features.tabgroup.presentation.navigation.mainGraph
import de.tabmates.features.tabgroup.presentation.navigation.mainSerializersModule
import kotlinx.serialization.modules.plus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import tabmatesapp.composeapp.generated.resources.back

private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = authSerializersModule + mainSerializersModule
}

// Deep-link definitions — mirrors the upcoming Nav3 navDeepLink<T>(basePath) API.
// TODO: Replace import with `androidx.navigation3.navDeepLink` when available.
private val deepLinks = listOf(
    navDeepLink<EmailVerification>(basePath = "${BuildKonfig.BASE_URL_HTTP}/api/auth/verify"),
    navDeepLink<ResetPassword>(basePath = "${BuildKonfig.BASE_URL_HTTP}/api/auth/reset-password"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(
                appModule,
                authenticationDataModule,
                coreDataModule,
                tabgroupDataModule,
                authPresentationModule,
                tabgroupPresentationModule,
            )
        }
    ) {
        TabMatesTheme {
            val mainViewModel = koinViewModel<MainViewModel>()
            val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()

            val startDestination = if (isLoggedIn) Home else Welcome
            val backStack = rememberNavBackStack(configuration = savedStateConfiguration, startDestination)
            val currentKey = backStack.lastOrNull()

            // Navigate to Welcome when session is invalidated (e.g. token refresh failed).
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn && currentKey is LoggedIn) {
                    backStack.clear()
                    backStack.add(Welcome)
                }
            }

            DisposableEffect(Unit) {
                DeepLinkHandler.listener = listener@{ uri ->
                    val navKey = resolveDeepLink(uri, deepLinks) ?: return@listener
                    backStack.clear()
                    backStack.add(Welcome)
                    backStack.add(navKey)
                }
                onDispose { DeepLinkHandler.listener = null }
            }

            val topLevelTabs = remember { listOf(Home, Activity, Group, Profile) }
            val entryDecorators = listOf<NavEntryDecorator<NavKey>>(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            )
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
                        entryDecorators = entryDecorators,
                        entryProvider = entryProvider {
                            mainGraph(backStack)
                        },
                    )
                }
            } else {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (currentKey != Welcome) {
                            TopAppBar(
                                title = { },
                                navigationIcon = {
                                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.ic_arrow_back),
                                            contentDescription = stringResource(Res.string.back),
                                        )
                                    }
                                },
                            )
                        }
                    },
                ) {
                    NavDisplay(
                        modifier = Modifier.fillMaxSize().padding(it),
                        backStack = backStack,
                        entryDecorators = entryDecorators,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            authGraph(
                                backStack = backStack,
                                onGuestClick = {
                                    backStack.clear()
                                    backStack.add(Home)
                                },
                                onLoginSuccess = {
                                    backStack.clear()
                                    backStack.add(Home)
                                },
                                snackbarHostState = snackbarHostState,
                            )
                        }
                    )
                }
            }
        }
    }
}
