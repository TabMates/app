---
name: android-compose-ui
description: |
  Compose UI patterns for TabMates KMP/CMP - stability, recomposition, side effects, lazy lists, animations, multi-preview annotations (@PreviewThemes), accessibility with Compose Resources, modifier extensions, and :core:designsystem components. Use this skill whenever writing or reviewing composables, optimizing recomposition, adding animations, creating previews, writing custom modifiers, or making any Compose UI decision beyond the ViewModel layer. Trigger on phrases like "composable", "recomposition", "LaunchedEffect", "Modifier", "LazyColumn", "preview", "animation", "design system", "stability", "contentDescription", "graphicsLayer", "slot API", or "Compose performance".
---

# Android / KMP Compose UI Patterns

## Core Principle

The UI is dumb. Composables render state and forward user actions — nothing more. All state lives in the ViewModel. All logic lives in the ViewModel, domain, or data layer. Compose code should contain zero business logic, zero data transformation, and minimal side effects.

---

## Stability & Recomposition

Strong skipping mode is enabled by default in modern Compose — no explicit opt-in needed.

Only annotate a state data class with `@Stable` when it contains fields the Compose compiler considers unstable (e.g., `List`, `Map`, `Set`, interfaces, or abstract types). If all fields are primitive types, `String`, or other stable types, no annotation is needed.

```kotlin
// Needs @Stable — contains a List (unstable by default)
@Stable
data class GroupOverviewState(
    val groups: List<GroupUi> = emptyList(),
    val isLoading: Boolean = false
)

// No annotation needed — all fields are stable
data class SettleUpState(
    val title: String = "",
    val isSaving: Boolean = false
)
```

---

## State Ownership

All state lives in the ViewModel. Do not use `remember` or `rememberSaveable` for application state — that belongs in the ViewModel's `StateFlow` and is surfaced via `collectAsStateWithLifecycle()`.

The only exception is Compose-internal state that the framework requires you to hold in composition, such as `LazyListState`, `ScrollState`, or `PagerState`. For these, use `remember*` as needed:

```kotlin
// Acceptable — Compose-owned UI state
val lazyListState = rememberLazyListState()

// Reacting to Compose-owned state with derivedStateOf
val showScrollToTop by remember {
    derivedStateOf { lazyListState.firstVisibleItemIndex > 5 }
}
```

`derivedStateOf` should only be used in these rare scenarios where Compose-internal state drives a derived value. If the derivation can happen in the ViewModel, it should.

Always collect ViewModel state with lifecycle awareness:
```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

---

## Side Effects

Side effects should be avoided when possible. If something can be handled by the ViewModel through an Action, do that instead of using a side effect in a composable.

When a side effect is truly necessary (e.g., interacting with Android lifecycle APIs that have no ViewModel equivalent), extract it into a dedicated composable to keep the Screen composable clean:

```kotlin
// Extracted into its own composable
@Composable
fun ObserveLifecycle(onStart: () -> Unit, onStop: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_STOP -> onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
```

`LaunchedEffect` is acceptable when genuinely needed, but question whether the work belongs in the ViewModel first. Avoid new custom `CompositionLocal`s — the only sanctioned one is `LocalTopBarActionsController` (`core:presentation` navigation chrome).

---

## Lazy Layouts

Add `key` to lazy list items when there is an obvious unique identifier available. Don't force it if it's unclear which property is unique:

```kotlin
LazyColumn {
    items(
        items = state.groups,
        key = { it.id }  // id is clearly unique
    ) { group ->
        GroupItem(group = group, onClick = { onGroupClick(group.id) })
    }
}
```

---

## Animations

Avoid animations that cause recompositions. Prefer approaches that animate below the recomposition layer:

- **`graphicsLayer`** — for alpha, scale, rotation, translation
- **Offset lambda** — for position changes (`offset { ... }`)
- **`Canvas`** — for custom drawing that animates
- **`animateFloatAsState` + `graphicsLayer`** — animate a float, apply in graphicsLayer

```kotlin
// Good — animates without recomposition
val alpha by animateFloatAsState(if (state.isVisible) 1f else 0f)
Box(
    modifier = Modifier.graphicsLayer { this.alpha = alpha }
)

// Bad — causes recomposition on every frame
Box(
    modifier = Modifier.alpha(animatedAlpha)
)
```

**Deferred state reads:** When a value drives an animation, pass it as a lambda rather than reading it directly. This defers the state read to the layout/draw phase and avoids recomposition:

```kotlin
// Good — deferred read
fun Modifier.animatedOffset(offsetProvider: () -> IntOffset) =
    offset { offsetProvider() }

// Bad — immediate read causes recomposition
fun Modifier.animatedOffset(offset: IntOffset) =
    offset(x = offset.x.dp, y = offset.y.dp)
```

---

## Modifier Extensions

Prefer plain `Modifier` extension functions or `Modifier.Node`-based factories. Do not make modifier extensions `@Composable`:

```kotlin
// Good — plain extension
fun Modifier.shimmerEffect(): Modifier = composed {
    // shimmer implementation
}

// Better — Modifier factory (no composition needed)
fun Modifier.roundedBackground(color: Color, radius: Dp) =
    background(color, RoundedCornerShape(radius))
```

---

## Design System & Slot APIs

The design system lives in `:core:designsystem` (`TabMatesTheme`, tokens, `TabMatesButton`, `TabMatesTextFieldLayout`, spacers, preview annotations). Reuse these atoms before writing new ones; use `MaterialTheme.colorScheme` / theme tokens, never hardcoded colors.

Use slot APIs (passing `@Composable` lambdas) primarily for design system components that need flexible content areas:

```kotlin
// Slot API — design system component
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card(modifier = modifier) {
        header()
        content()
    }
}
```

Feature-level composables should prefer typed parameters over slots for clarity.

---

## Previews

Use the multi-preview annotations from `:core:designsystem` (`core/designsystem/.../preview/DevicePreviews.kt`) instead of raw `@Preview`:

- **`@PreviewThemes`** — light + dark. Preferred for most components.
- **`@PreviewPhones`** — phone portrait + landscape.
- **`@PreviewScreenSizes`** — phone, foldable, tablet, desktop, web.
- **`@PreviewAll`** — every theme+size combo (14 previews). Use sparingly.

Wrap in `TabMatesTheme` (+ `Surface` if a background is needed), with realistic sample data:

```kotlin
@PreviewThemes
@Composable
private fun GroupDetailScreenPreview() {
    TabMatesTheme {
        Surface {
            GroupDetailScreen(state = GroupDetailState(/* realistic data */), ...)
        }
    }
}
```

---

## Accessibility

Use meaningful `contentDescription` on all interactive or informational visual elements. Always use Compose Resources (`Res.string.*` via `org.jetbrains.compose.resources.stringResource` — there is no `R.string` in this project):

```kotlin
Icon(
    imageVector = vectorResource(Res.drawable.ic_add),
    contentDescription = stringResource(Res.string.create_group)
)
```

Strings live in each module's `composeResources/values/strings.xml`.

For decorative elements that convey no information, set `contentDescription = null`.

---

## TextField

Text input uses Compose `TextFieldState` held inside the ViewModel's state — NOT `value`/`onValueChange` with per-keystroke actions:

```kotlin
// State
data class LoginState(val emailTextFieldState: TextFieldState = TextFieldState(), ...)

// Screen composable — prefer the designsystem wrapper
TabMatesTextFieldLayout(state = state.emailTextFieldState, ...)
```

The ViewModel derives validation with `snapshotFlow { state.value.emailTextFieldState.text }` — see the **android-presentation-mvi** skill for the full pattern.
