package de.tabmates.composeapp

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.platform.LocalDensity
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
import de.tabmates.composeapp.navigation.PlatformBackHandler
import de.tabmates.composeapp.navigation.rememberScreenTopBarNavEntryDecorator
import de.tabmates.composeapp.promo.AppPromoBannerRoot
import de.tabmates.composeapp.promo.isAndroidBrowser
import de.tabmates.composeapp.session.ReauthRoot
import de.tabmates.composeapp.session.SessionExpiredBanner
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
import de.tabmates.features.authentication.presentation.forgotpassword.ForgotPasswordScreenRoot
import de.tabmates.features.authentication.presentation.navigation.EmailVerification
import de.tabmates.features.authentication.presentation.navigation.Reauth
import de.tabmates.features.authentication.presentation.navigation.ReauthForgotPassword
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
        // Re-apply status/navigation bar appearance on every theme change so the bars follow
        // the in-app `ThemeMode` override (Light/Dark/System) instead of the OS dark-mode flag.
        ApplySystemBars(darkTheme = darkTheme)
        TabMatesTheme(darkTheme = darkTheme) {
            if (!rememberResourcesPrimed()) return@TabMatesTheme

            // Checks for app updates on launch: native Play in-app update on eligible Android
            // devices, store-redirect dialog everywhere else.
            AppUpdateGate()

            val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()

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

            // An expired session still counts: its data is on the device, so the app opens on it
            // rather than throwing the user back to the welcome screen.
            val startDestination = if (sessionState.hasLocalSession) Home else Welcome
            val backStack = rememberNavBackStack(configuration = savedStateConfiguration, startDestination)
            val currentKey = backStack.lastOrNull()

            // Bridge the browser Back button to the Nav3 back stack (web only; no-op on other
            // targets). Enabled only when there is something to pop — at the root, Back keeps
            // the user in the app instead of unloading it.
            PlatformBackHandler(
                enabled = backStack.size > 1,
                onBack = { backStack.removeLastOrNull() },
            )

            // Navigate to Welcome when the account is really gone (signed out, deleted). An
            // *expired* session deliberately does not land here: the shell branch below keys off
            // `currentKey is LoggedIn`, so staying put is what keeps the app usable on local data
            // behind the re-auth banner.
            ObserveAsEvents(mainViewModel.sessionState) {
                if (!it.hasLocalSession && currentKey is LoggedIn) {
                    backStack.clear()
                    backStack.add(Welcome)
                }
            }

            DisposableEffect(Unit) {
                DeepLinkHandler.listener = listener@{ uri ->
                    val navKey = resolveDeepLink(uri, deepLinks) ?: return@listener
                    if (navKey is LoggedIn && !mainViewModel.sessionState.value.hasLocalSession) {
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
            val isAndroidBrowser = remember { isAndroidBrowser() }

            if (currentKey is LoggedIn) {
                val topBarActions = remember { TopBarActionsController() }
                // Hide the navigation bar while typing: the keyboard already owns the bottom of
                // the screen, and stacking the bar on top of it would eat another ~88dp from the
                // screen the user is filling in.
                val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                val navigationSuiteType =
                    if (isKeyboardOpen) {
                        NavigationSuiteType.None
                    } else {
                        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
                    }
                NavigationSuiteScaffold(
                    // Lifts the whole shell above the keyboard. Applied here, at the node flush
                    // with the window, so the padding equals the keyboard height exactly; further
                    // in it would sit above the navigation bar and over-pad by that bar's height.
                    modifier = Modifier.imePadding(),
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
                            if (sessionState == SessionShellState.Stale) {
                                SessionExpiredBanner(
                                    onSignInClick = { backStack.add(Reauth) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                ConnectivityBannerRoot(modifier = Modifier.fillMaxWidth())
                            }
                            // Web-only, and never while the session is stale: the re-auth banner
                            // above is the one thing the user has to act on, so nothing else gets
                            // to ask for a tap at the same time.
                            if (isAndroidBrowser && sessionState != SessionShellState.Stale) {
                                AppPromoBannerRoot(modifier = Modifier.fillMaxWidth())
                            }
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
                                        // Registered here rather than in `authGraph`: re-auth is
                                        // reached *from* the logged-in shell, which is served by
                                        // this entry provider.
                                        entry<Reauth> {
                                            ReauthRoot(
                                                backStack = backStack,
                                                snackbarHostState = snackbarHostState,
                                            )
                                        }
                                        entry<ReauthForgotPassword> {
                                            ForgotPasswordScreenRoot()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    // safeDrawing covers the keyboard as well as the system bars, so the auth
                    // screens keep their content above whichever is showing.
                    contentWindowInsets = WindowInsets.safeDrawing,
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
