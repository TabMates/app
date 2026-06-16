package de.tabmates.composeapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import de.tabmates.composeapp.deeplink.navDeepLink
import de.tabmates.composeapp.deeplink.resolveDeepLink
import de.tabmates.composeapp.di.TabMatesKoinApp
import de.tabmates.composeapp.navigation.ScreenTopBar
import de.tabmates.composeapp.sync.CurrencySyncCoordinator
import de.tabmates.composeapp.sync.GroupSyncCoordinator
import de.tabmates.composeapp.sync.NotificationsSyncCoordinator
import de.tabmates.composeapp.update.AppUpdateGate
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.core.presentation.navigation.FabAction
import de.tabmates.core.presentation.navigation.LocalTopBarActionsController
import de.tabmates.core.presentation.navigation.LoggedIn
import de.tabmates.core.presentation.navigation.ScreenWithFab
import de.tabmates.core.presentation.navigation.ScreenWithTopBar
import de.tabmates.core.presentation.navigation.TopBarActionsController
import de.tabmates.core.presentation.navigation.TopLevelTab
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.authentication.presentation.navigation.EmailVerification
import de.tabmates.features.authentication.presentation.navigation.ResetPassword
import de.tabmates.features.authentication.presentation.navigation.Welcome
import de.tabmates.features.authentication.presentation.navigation.authGraph
import de.tabmates.features.authentication.presentation.navigation.authSerializersModule
import de.tabmates.features.notifications.domain.NotificationDeepLinkBus
import de.tabmates.features.tabgroup.presentation.navigation.Activity
import de.tabmates.features.tabgroup.presentation.navigation.AddExpense
import de.tabmates.features.tabgroup.presentation.navigation.CreateGroup
import de.tabmates.features.tabgroup.presentation.navigation.Group
import de.tabmates.features.tabgroup.presentation.navigation.GroupDetail
import de.tabmates.features.tabgroup.presentation.navigation.Home
import de.tabmates.features.tabgroup.presentation.navigation.JoinGroup
import de.tabmates.features.tabgroup.presentation.navigation.Profile
import de.tabmates.features.tabgroup.presentation.navigation.mainGraph
import de.tabmates.features.tabgroup.presentation.navigation.mainSerializersModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.modules.plus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel
import org.koin.plugin.module.dsl.koinConfiguration
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.back
import tabmatesapp.composeapp.generated.resources.create_group
import tabmatesapp.composeapp.generated.resources.ic_add
import tabmatesapp.composeapp.generated.resources.ic_arrow_back

private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = authSerializersModule + mainSerializersModule
}

// Deep-link definitions — mirrors the upcoming Nav3 navDeepLink<T>(basePath) API.
// TODO: Replace import with `androidx.navigation3.navDeepLink` when available.
private val deepLinks = listOf(
    navDeepLink<EmailVerification>(basePath = "${BuildKonfig.BASE_URL_HTTP}/api/auth/verify"),
    navDeepLink<ResetPassword>(basePath = "${BuildKonfig.BASE_URL_HTTP}/api/auth/reset-password"),
    navDeepLink<JoinGroup>(basePath = "${BuildKonfig.BASE_URL_HTTP}/j", pathSuffixParam = "token"),
    navDeepLink<GroupDetail>(basePath = "${BuildKonfig.BASE_URL_HTTP}/groups", pathSuffixParam = "groupId"),
)

@get:Composable
private val entryDecorators
    get() = listOf<NavEntryDecorator<NavKey>>(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration<TabMatesKoinApp>(),
    ) {
        val mainViewModel = koinViewModel<MainViewModel>()
        val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
        val darkTheme =
            when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
        TabMatesTheme(darkTheme = darkTheme) {
            // Checks for app updates on launch: native Play in-app update on eligible Android
            // devices, store-redirect dialog everywhere else.
            AppUpdateGate()

            val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()

            // Start sync coordinators off the main thread, post-first-frame.
            // Keeps KSafe construction + repository graph off the startup critical path.
            val koin = getKoin()
            LaunchedEffect(Unit) {
                withContext(Dispatchers.Default) {
                    koin.get<GroupSyncCoordinator>()
                    koin.get<CurrencySyncCoordinator>()
                    koin.get<NotificationsSyncCoordinator>()
                }
            }

            // Forward deep links from clicked notifications to the deep-link handler.
            LaunchedEffect(Unit) {
                koin.get<NotificationDeepLinkBus>().deepLinks.collect { uri ->
                    DeepLinkHandler.onDeepLink(uri)
                }
            }

            val startDestination = if (isLoggedIn) Home else Welcome
            val backStack = rememberNavBackStack(configuration = savedStateConfiguration, startDestination)
            val currentKey = backStack.lastOrNull()

            // Navigate to Welcome when session is invalidated (e.g. token refresh failed).
            ObserveAsEvents(mainViewModel.isLoggedIn) {
                if (!it && currentKey is LoggedIn) {
                    backStack.clear()
                    backStack.add(Welcome)
                }
            }

            DisposableEffect(Unit) {
                DeepLinkHandler.listener = listener@{ uri ->
                    val navKey = resolveDeepLink(uri, deepLinks) ?: return@listener
                    if (navKey is LoggedIn && !mainViewModel.isLoggedIn.value) {
                        mainViewModel.setPendingPostAuthNavKey(navKey)
                        backStack.clear()
                        backStack.add(Welcome)
                    } else {
                        backStack.clear()
                        if (navKey is LoggedIn) {
                            backStack.add(Home)
                        } else {
                            backStack.add(Welcome)
                        }
                        backStack.add(navKey)
                    }
                }
                onDispose {
                    DeepLinkHandler.listener = null
                }
            }

            val topLevelTabs = remember { listOf(Home, Activity, Group, Profile) }
            val snackbarHostState = remember { SnackbarHostState() }

            if (currentKey is LoggedIn) {
                val topBarActions = remember { TopBarActionsController() }
                NavigationSuiteScaffold(
                    navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2()),
                    navigationItems = {
                        topLevelTabs.forEach { tab ->
                            val selected = currentKey == tab
                            NavigationSuiteItem(
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
                    primaryActionContent = {
                        (currentKey as? ScreenWithFab)?.let { screen ->
                            FloatingActionButton(
                                modifier = Modifier.padding(start = 16.dp),
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = {
                                    when (val action = screen.fabAction) {
                                        FabAction.CreateGroup -> backStack.add(CreateGroup)
                                        is FabAction.AddExpense -> backStack.add(AddExpense(action.groupId))
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.ic_add),
                                    contentDescription = stringResource(Res.string.create_group),
                                )
                            }
                        }
                    },
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            val override = topBarActions.overrideFor(currentKey)
                            if (override != null) {
                                ScreenTopBar(
                                    title = override.title,
                                    navigationAction = override.navigationAction,
                                    onNavigationClick = override.onNavigationClick,
                                    actions = override.actions,
                                )
                            } else {
                                (currentKey as? ScreenWithTopBar)?.let { config ->
                                    ScreenTopBar(
                                        title = config.topBarTitle,
                                        navigationAction = config.topBarAction,
                                        onNavigationClick = { backStack.removeLastOrNull() },
                                        actions = { topBarActions.actionsFor(currentKey)?.invoke() },
                                    )
                                }
                            }
                        },
                    ) {
                        CompositionLocalProvider(LocalTopBarActionsController provides topBarActions) {
                            NavDisplay(
                                modifier = Modifier.fillMaxSize().padding(it),
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                entryDecorators = entryDecorators,
                                entryProvider = entryProvider {
                                    mainGraph(
                                        backStack = backStack,
                                        snackbarHostState = snackbarHostState,
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
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
                                    mainViewModel.consumePendingPostAuthNavKey()?.let(backStack::add)
                                },
                                onLoginSuccess = {
                                    backStack.clear()
                                    backStack.add(Home)
                                    mainViewModel.consumePendingPostAuthNavKey()?.let(backStack::add)
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
