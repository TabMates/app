---
name: android-navigation
description: |
  Navigation 3 (Nav3) for TabMates KMP/CMP - NavKey route objects, EntryProviderScope feature graphs, NavBackStack mutation, SerializersModule registration, NavDisplay wiring in :composeApp, deep links. Use this skill whenever adding a screen/route, navigating between screens, wiring a nav graph, or handling deep links. Trigger on phrases like "add a route", "add a screen", "navigate", "nav graph", "NavKey", "backStack", "NavDisplay", "EntryProviderScope", "deep link", or "type-safe nav".
---

# TabMates Navigation (Navigation 3)

This project uses **androidx.navigation3** — NOT Navigation-Compose. There is no `NavController`, `NavHost`, `NavGraphBuilder`, or `composable<Route>`. Navigation = mutating a `NavBackStack<NavKey>`.

## Principles

- Routes are `@Serializable` objects/data classes extending `LoggableNavKey()` in the feature's `presentation` module.
- Each feature exposes an `EntryProviderScope<NavKey>.<feature>Graph(backStack, snackbarHostState)` extension.
- Every route must be registered in the feature's polymorphic `SerializersModule` (back stack persistence) — forgetting this crashes on state save.
- Cross-screen navigation = `backStack.add(...)` / `backStack.removeLastOrNull()` / `backStack.removeAll { ... }`.
- All graphs + serializer modules wired once in `composeApp/src/commonMain/kotlin/de/tabmates/composeapp/App.kt` via `NavDisplay`.

## Route Objects

Routes implement screen-contract interfaces from `core/presentation/.../navigation/` (`TopLevelTab`, `LoggedIn`, `ScreenWithTopBar`, `ScreenWithFab`) so scaffold chrome (bottom bar, top bar, FAB) is data-driven:

```kotlin
// features/tabgroup/presentation/.../navigation/MainNavKeys.kt
@Serializable
data object Home : LoggableNavKey(), TopLevelTab, ScreenWithFab {
    override val icon: ImageVector @Composable get() = vectorResource(Res.drawable.ic_home)
    override val selectedIcon: ImageVector @Composable get() = vectorResource(Res.drawable.ic_home_filled)
    override val label: UiText = UiText.Resource(Res.string.home_label)
    override val fabAction: FabAction = FabAction.CreateGroup
}

@Serializable
data class GroupDetail(val groupId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}
```

Use `data object` for parameterless screens, `data class` for screens with arguments. Pass IDs, not complex objects.

## SerializersModule Registration (required!)

```kotlin
// features/tabgroup/presentation/.../navigation/MainGraph.kt
val mainSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(Home::class)
        subclass(GroupDetail::class)
        // ... every NavKey of the feature
    }
}
```

`App.kt` combines them: `SavedStateConfiguration { serializersModule = authSerializersModule + mainSerializersModule }`.

## Feature Graph

```kotlin
// MainGraph.kt
fun EntryProviderScope<NavKey>.mainGraph(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
) {
    entry<Home> {
        HomeRoot(
            onGroupClick = { groupId -> backStack.add(GroupDetail(groupId)) },
        )
    }
    entry<GroupDetail> { route ->
        GroupDetailRoot(
            groupId = route.groupId,
            snackbarHostState = snackbarHostState,
            onSettingsClick = { backStack.add(GroupSettings(route.groupId)) },
        )
    }
}
```

Back-stack idioms seen in the codebase:
- Close current screen: `backStack.removeLastOrNull()`
- Close all instances after save: `backStack.removeAll { it is AddExpense }`
- Switch top-level tab: `backStack.removeAll { it is TopLevelTab }; backStack.add(Group)`

Root composables navigate only via lambdas passed in from the graph — never by touching the back stack directly.

## Wiring in :composeApp

`App.kt` creates `rememberNavBackStack(...)`, entry decorators (`rememberSaveableStateHolderNavEntryDecorator`, `rememberViewModelStoreNavEntryDecorator`) and a `NavDisplay` with `entryProvider { authGraph(...); mainGraph(...) }`. Study `composeApp/src/commonMain/kotlin/de/tabmates/composeapp/App.kt` before changing wiring.

## Deep Links

Defined in `App.kt` with the local `navDeepLink<T>(basePath, pathSuffixParam)` helper (`composeApp/.../deeplink/NavDeepLink.kt`), resolved by `DeepLinkHandler` / `resolveDeepLink`. URLs are built from `BuildKonfig.BASE_URL_HTTP`. Tests: `composeApp/src/commonTest/.../deeplink/`.

## Checklist: Adding a Screen

- [ ] `@Serializable` NavKey in feature `presentation` (`MainNavKeys.kt`-style file), implementing the right chrome interfaces (`LoggedIn`, `ScreenWithTopBar`/`ScreenWithFab`, `TopLevelTab`)
- [ ] Register with `subclass(...)` in the feature's `SerializersModule`
- [ ] Add `entry<NewKey> { ... }` to the feature graph, wiring callbacks to `backStack` mutations
- [ ] Add string resources for `topBarTitle`/`label`
- [ ] If externally reachable: add a `navDeepLink<T>` in `App.kt`
