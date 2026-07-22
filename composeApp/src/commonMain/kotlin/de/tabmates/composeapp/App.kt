package de.tabmates.composeapp

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import de.tabmates.composeapp.connectivity.ConnectivityBannerRoot
import de.tabmates.composeapp.deeplink.DeepLinkHandler
import de.tabmates.composeapp.deeplink.navDeepLink
import de.tabmates.composeapp.deeplink.resolveDeepLink
import de.tabmates.composeapp.di.TabMatesKoinApp
import de.tabmates.composeapp.lock.AppLockViewModel
import de.tabmates.composeapp.lock.BiometricLockGate
import de.tabmates.composeapp.lock.rememberAppLockViewModel
import de.tabmates.composeapp.navigation.rememberScreenTopBarNavEntryDecorator
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
import de.tabmates.features.tabgroup.presentation.navigation.AddEntry
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
import tabmatesapp.composeapp.generated.resources.add_entry
import tabmatesapp.composeapp.generated.resources.create_group
import tabmatesapp.composeapp.generated.resources.ic_add

private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = authSerializersModule + mainSerializersModule
}

// Deep-link definitions — mirrors the upcoming Nav3 navDeepLink<T>(basePath) API.
// TODO: Replace import with `androidx.navigation3.navDeepLink` when available.
// Matched against the user-facing public host (BASE_URL_PUBLIC, e.g. https://app.tabmates.de),
// not the backend API host — the same URL an installed app (App Links) and the web client resolve.
private val deepLinks = listOf(
    navDeepLink<EmailVerification>(basePath = "${BuildKonfig.BASE_URL_PUBLIC}/api/auth/verify"),
    navDeepLink<ResetPassword>(basePath = "${BuildKonfig.BASE_URL_PUBLIC}/api/auth/reset-password"),
    navDeepLink<JoinGroup>(basePath = "${BuildKonfig.BASE_URL_PUBLIC}/j", pathSuffixParam = "token"),
    navDeepLink<GroupDetail>(basePath = "${BuildKonfig.BASE_URL_PUBLIC}/groups", pathSuffixParam = "groupId"),
)

@Composable
private fun rememberEntryDecorators(backStack: NavBackStack<NavKey>): List<NavEntryDecorator<NavKey>> =
    listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        // Last = innermost: the top bar composes inside the saveable-state/ViewModel scopes.
        rememberScreenTopBarNavEntryDecorator(backStack),
    )

private const val NAV_TRANSITION_DURATION_MS = 300
private const val PREDICTIVE_POP_EXIT_TARGET_SCALE = 0.92f

private val navTransition: ContentTransform
    get() =
        ContentTransform(
            fadeIn(tween(NAV_TRANSITION_DURATION_MS)),
            fadeOut(tween(NAV_TRANSITION_DURATION_MS)),
        )

// All sub-animations share one duration: the predictive gesture scrubs the whole
// ContentTransform through a SeekableTransitionState, and mismatched durations hitch on settle.
private val predictivePopTransition: ContentTransform
    get() =
        ContentTransform(
            targetContentEnter = fadeIn(tween(NAV_TRANSITION_DURATION_MS)),
            initialContentExit =
                scaleOut(
                    targetScale = PREDICTIVE_POP_EXIT_TARGET_SCALE,
                    animationSpec = tween(NAV_TRANSITION_DURATION_MS),
                ) + fadeOut(tween(NAV_TRANSITION_DURATION_MS)),
        )

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
        // Re-apply status/navigation bar appearance on every theme change so the bars follow
        // the in-app `ThemeMode` override (Light/Dark/System) instead of the OS dark-mode flag.
        ApplySystemBars(darkTheme = darkTheme)
        TabMatesTheme(darkTheme = darkTheme) {
            val appLockViewModel = rememberAppLockViewModel()
            // Gates all app content behind the biometric lock when the user has enabled it.
            BiometricLockGate(viewModel = appLockViewModel) {
                AppRoot(
                    mainViewModel = mainViewModel,
                    appLockViewModel = appLockViewModel,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    mainViewModel: MainViewModel,
    appLockViewModel: AppLockViewModel,
) {
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
    val appScope = rememberCoroutineScope()

    if (currentKey is LoggedIn) {
        val topBarActions = remember { TopBarActionsController() }
        val navigationSuiteType =
            NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
        NavigationSuiteScaffold(
            navigationSuiteType = navigationSuiteType,
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
                val fabScreen = currentKey as? ScreenWithFab
                if (fabScreen != null) {
                    FloatingActionButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = {
                            when (val action = fabScreen.fabAction) {
                                FabAction.CreateGroup -> backStack.add(CreateGroup)
                                is FabAction.AddEntry -> backStack.add(AddEntry(action.groupId))
                            }
                        },
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_add),
                            contentDescription = when (fabScreen.fabAction) {
                                FabAction.CreateGroup -> stringResource(Res.string.create_group)
                                is FabAction.AddEntry -> stringResource(Res.string.add_entry)
                            },
                        )
                    }
                } else if (navigationSuiteType != NavigationSuiteType.NavigationBar) {
                    // Reserve the FAB's footprint so the navigation rail/drawer items don't
                    // jump vertically when the FAB is shown/hidden across tabs.
                    Spacer(modifier = Modifier.padding(start = 16.dp).size(56.dp))
                }
            },
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    ConnectivityBannerRoot(modifier = Modifier.fillMaxWidth())
                    CompositionLocalProvider(LocalTopBarActionsController provides topBarActions) {
                        NavDisplay(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            backStack = backStack,
                            onBack = { backStack.removeLastOrNull() },
                            entryDecorators = rememberEntryDecorators(backStack),
                            transitionSpec = { navTransition },
                            popTransitionSpec = { navTransition },
                            predictivePopTransitionSpec = { _ -> predictivePopTransition },
                            entryProvider = entryProvider {
                                mainGraph(
                                    backStack = backStack,
                                    snackbarHostState = snackbarHostState,
                                    appScope = appScope,
                                )
                            },
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) {
            NavDisplay(
                modifier = Modifier.fillMaxSize().padding(it),
                backStack = backStack,
                entryDecorators = rememberEntryDecorators(backStack),
                transitionSpec = { navTransition },
                popTransitionSpec = { navTransition },
                predictivePopTransitionSpec = { _ -> predictivePopTransition },
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    authGraph(
                        backStack = backStack,
                        onGuestClick = {
                            // Mark unlocked so the just-authenticated session isn't re-locked.
                            appLockViewModel.onSignedIn()
                            backStack.clear()
                            backStack.add(Home)
                            mainViewModel.consumePendingPostAuthNavKey()?.let(backStack::add)
                        },
                        onLoginSuccess = {
                            appLockViewModel.onSignedIn()
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
