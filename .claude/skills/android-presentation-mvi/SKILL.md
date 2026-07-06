---
name: android-presentation-mvi
description: |
  TabMates presentation layer - State/StateFlow ViewModels with stateIn+WhileSubscribed, TextFieldState for text input, Channel events with ObserveAsEvents, Root/Screen composable split, UiText, @KoinViewModel. Use this skill whenever creating or reviewing a ViewModel, defining screen state or events, structuring screen composables, handling text input, or mapping errors to UI strings. Trigger on phrases like "ViewModel", "create a screen", "screen state", "event", "UiText", "TextFieldState", "ObserveAsEvents", "Root composable", or "collectAsStateWithLifecycle".
---

# TabMates Presentation Layer

Every screen has: a **State** data class, a **ViewModel** (`StateFlow<State>` + optional one-time **Event** flow), and a **Root/Screen** composable pair.

## State

```kotlin
data class ForgotPasswordState(
    val emailTextFieldState: TextFieldState = TextFieldState(),  // text input lives here
    val canSubmit: Boolean = false,
    val isLoading: Boolean = false,
    val isEmailSentSuccessfully: Boolean = false,
    val errorText: UiText? = null,
)
```

Update with `_state.update { it.copy(...) }` — never replace the flow.

## Text Input: TextFieldState (not value/onValueChange)

Text fields hold a Compose `TextFieldState` inside the screen state. No per-keystroke actions. Derive validation with `snapshotFlow`:

```kotlin
private val isEmailValidFlow =
    snapshotFlow { state.value.emailTextFieldState.text.toString() }
        .map { email -> EmailValidator.validate(email) }
        .distinctUntilChanged()
```

## ViewModel (real shape — `ForgotPasswordViewModel.kt`)

```kotlin
@KoinViewModel
class ForgotPasswordViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeValidationState()
                hasLoadedInitialData = true
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = _state.value,
        )

    fun submitForgotPasswordRequest() {
        if (state.value.isLoading || !state.value.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorText = null) }
            authService.forgotPassword(state.value.emailTextFieldState.text.toString())
                .onSuccess { _state.update { it.copy(isEmailSentSuccessfully = true, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(errorText = error.toUiText(), isLoading = false) } }
        }
    }
}
```

Conventions:
- `@KoinViewModel`, constructor-inject domain interfaces.
- `stateIn(WhileSubscribed(5_000))` + `hasLoadedInitialData` guard for lazy initial load.
- **User intents = direct public functions** (dominant style). A sealed `Action` + `onAction()` variant exists in two screens (`GroupSettingsViewModel`, `JoinGroupViewModel`) — match whichever style the feature already uses.
- One-time events (snackbar, navigation): `private val eventChannel = Channel<XEvent>()` + `val events = eventChannel.receiveAsFlow()`; consume with `ObserveAsEvents` (`core:presentation`).

## Root / Screen Split

Root (suffix `Root` or `ScreenRoot`) owns the ViewModel and navigation callbacks; Screen is stateless and previewable. Both in one file.

```kotlin
@Composable
fun ForgotPasswordScreenRoot(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        state = state,
        onSubmit = viewModel::submitForgotPasswordRequest,
        onBack = onBack,
    )
}
```

- Always `collectAsStateWithLifecycle()`.
- Never pass ViewModels down the tree; inject at Root only.
- Navigation callbacks come from the feature graph (see android-navigation skill) — Roots never touch the back stack.

## UiText (`core/presentation/.../util/UiText.kt`)

```kotlin
sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class Resource(val id: StringResource, val args: Array<Any> = arrayOf()) : UiText  // Compose Resources
}
```

- Resource-backed or localizable strings → `UiText.Resource(Res.string.key)`. Always-dynamic values (usernames, formatted amounts) → plain `String`.
- Render with `uiText.asString()` (Composable). Map errors via `toUiText()` (see android-data-layer skill).

## Process Death

For critical form input, restore essentials via `SavedStateHandle` constructor param; instantiate directly in tests (`SavedStateHandle(mapOf("id" to "123"))`). Save only what matters, not whole state.

## Checklist: New Screen

- [ ] `State` data class (TextFieldState for inputs) + ViewModel with `@KoinViewModel`
- [ ] `stateIn(WhileSubscribed(5_000))` + `hasLoadedInitialData` pattern
- [ ] Events via `Channel` + `ObserveAsEvents` if needed
- [ ] `<X>Root` (ViewModel + callbacks) and `<X>Screen` (stateless, previewable) in one file
- [ ] Errors mapped to `UiText` via `toUiText()`
- [ ] NavKey + graph entry + serializer registration (android-navigation skill)
