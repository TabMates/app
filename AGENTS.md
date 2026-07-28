# TabMates AI Agent Guide

Doc: TabMates architecture, patterns, guidelines for AI agents.

## 1. Project Overview
- **Tech Stack:** Kotlin Multiplatform (KMP), Compose Multiplatform (CMP).
- **Architecture:** Clean Architecture + MVVM.
- **Dependency Injection:** Koin.
- **Navigation:** Navigation 3 (Nav3).
- **Networking:** Ktor with ContentNegotiation (Serialization).
- **Database:** Room (KMP).
- **Targets:** Android, iOS, Desktop (JVM), Web (WasmJS).
- **Package Root:** `de.tabmates`.

---

## 2. Module Structure & Hierarchy

Project modularized by feature and layer. Use **typesafe project accessors** (e.g., `projects.core.domain`).

### Core Modules (`:core:*`)
- **`:core:domain`**: Pure Kotlin. Business models, standard `Result<D, E>` type, `Error` interfaces, global loggers.
- **`:core:data`**: Shared networking setup (`HttpClientFactory`), standard Ktor configs, common data sources.
- **`:core:presentation`**: Shared UI logic, `UiText` for localized strings, `ObserveAsEvents` for one-time events.
- **`:core:designsystem`**: Shared Compose tokens (Color, Type, Shape), reusable atomic components (Buttons, TextFields, etc.).

### Feature Modules (`:features:*:*`)
Each feature split into:
- **`:domain`**: Interfaces (`Service`, `Repository`), business models, validators.
- **`:data`**: Domain interface impls, DTOs, mappers, API services.
- **`:presentation`**: Compose UI (Screens, Components), ViewModels, navigation routes.
- **`:database`** (Optional): Room database, entities, DAOs, migrations (e.g. `:features:tabgroup:database`).
- **`:testing`** (Optional): Shared fakes for tests (e.g. `FakeAuthService` in `:features:authentication:testing`).

Not all features have all layers (`:features:appupdate` = domain + data only). Special: `:features:tabgroup:sqliteWasmWorker` (web SQLite worker).

### Application Modules
- **`:composeApp`**: Main entry point for shared UI. Aggregates all features, wires Navigation/DI.
- **`:androidApp`**: Android-specific config, depends only on `:composeApp`.

---

## 3. Architecture Layers

### Domain Layer (Pure Kotlin)
- Define `interface AuthService` or `interface GroupRepository`.
- Use `de.tabmates.core.domain.util.Result<D, E>` for all operation outcomes.
- Models: data classes, ideally immutable.

### Data Layer
- Implement domain interfaces.
- Use `Ktor` for networking.
- Use `Mappers` to convert DTOs to domain models.
- **Convention:** DTOs suffix with `Request` or `Response`.

### Presentation Layer (Compose Multiplatform)
- **ViewModels:**
    - Use `StateFlow` for UI state (e.g., `state: StateFlow<LoginState>`). Pattern: `stateIn(viewModelScope, WhileSubscribed(5_000), initial)` with `onStart { }` + `hasLoadedInitialData` guard.
    - Use `Channel` + `receiveAsFlow()` for one-time events (e.g., `events: Flow<LoginEvent>`).
    - Text input: hold Compose `TextFieldState` inside state; validate via `snapshotFlow { textFieldState.text }`. No per-keystroke actions.
    - User intents: direct public functions (dominant style, e.g. `submitForgotPasswordRequest()`); two screens use sealed `Action` + `onAction()` (GroupSettings, JoinGroup). Match the style of the feature you touch.
    - Annotate with `@KoinViewModel`. Inherit from `androidx.lifecycle.ViewModel`.
- **UI Components:**
    - `Root` composables (e.g., `LoginRoot`) handle ViewModel interaction and event observation.
    - Screen composables (e.g., `LoginScreen`) stateless, take data/callbacks.
    - Use `ObserveAsEvents` to handle ViewModel events (Snackbars, Navigation).

---

## 4. Navigation (Navigation 3)

- **Routes:** `@Serializable` data classes/objects in feature's `presentation` module (e.g., `data object Home : NavKey`).
- **Graphs:** Features define `EntryProviderScope<NavKey>.featureGraph` extension.
- **Wiring:** All feature graphs aggregated in `composeApp/App.kt` via `NavDisplay`.
- **Top-level Tabs:** Implement `TopLevelTab` and `LoggedIn` interfaces for consistent bottom bar behavior.

---

## 5. Dependency Injection (Koin Annotations + KSP)

**No Koin DSL** (`module { }`, `singleOf`, `viewModelOf`) — project uses Koin Annotations:
- Per layer: `@Module @Configuration @ComponentScan("<package>") class FeatureLayerModule` (see `features/authentication/data/.../di/AuthenticationDataModule.kt`).
- Bindings: `@Single` (add `binds = [Interface::class]` when impl name ≠ interface), `@KoinViewModel` on ViewModels.
- Platform deps: `expect class PlatformXyzModule()` in commonMain + `actual` per source set (see `core/data/.../di/PlatformCoreDataModule.kt` + `.android/.desktop/.native/.web` variants).
- Assembly: `@KoinApplication class TabMatesKoinApp` in `composeApp/.../di/AppModule.kt`; started in `App()` via `KoinApplication(configuration = koinConfiguration<TabMatesKoinApp>())`.
- Use `koinViewModel()` in Root composables.

---

## 6. Design System & Theming

- **Theme:** `TabMatesTheme` (built on Material3).
- **Tokens:** In `:core:designsystem`. Use `MaterialTheme.colorScheme` or custom `TabMatesTheme` properties.
- **Resources:** Use `Res.string.key` or `Res.drawable.key` via Compose Resources.
- **Localization:** Managed in `composeResources/values/strings.xml` within each module.

---

## 7. Compose Previews

Use multi-preview annotations from `:core:designsystem` for consistent testing across themes and devices.

### Multi-preview Annotations
- **`@PreviewThemes`**: Light and Dark mode previews. **Preferred for most components.**
- **`@PreviewPhones`**: Portrait and Landscape previews for phones.
- **`@PreviewScreenSizes`**: Phone, Foldable, Tablet, Desktop, Web previews.
- **`@PreviewAll`**: Every theme+screen combination (14 previews). Use sparingly.

### Pattern
Wrap previewed component in `TabMatesTheme` and `Surface` (if needed for background).
```kotlin
@PreviewThemes
@Composable
private fun MyComponentPreview() {
    TabMatesTheme {
        Surface {
            MyComponent()
        }
    }
}
```

---

## 8. Convention Plugins (build-logic)

**NEVER** configure KMP manually. Plugin IDs (prefix `de.tabmates.convention.`):
- `kmp.library`: Standard KMP library (domain/data modules).
- `cmp.library`: CMP library with Compose dependencies.
- `cmp.feature`: Feature presentation module (includes VM, Lifecycle, Core Presentation).
- `cmp.application`: `:composeApp` shared-UI application.
- `cmp.resources`: Compose Resources generation.
- `android.application` / `android.application.compose`: `:androidApp`.
- `room`: Room with KSP.
- `koin`: Koin Annotations + KSP compiler.
- `ktlint`: ktlint checks.
- `buildkonfig`: BuildConfig-like constants (`BuildKonfig`).

Registrations: `build-logic/convention/build.gradle.kts`.

---

## 9. Source Set Hierarchy & Platform Code

Custom hierarchy template in `build-logic` (`HierarchyTemplate.kt`):
```
common
├── mobile  (android + ios)
├── web     (wasmJs)
├── native  (ios + macos)
│   └── apple → ios, macos
└── desktop (jvm)
```
- **Ktor engines:** `okhttp` (android), `darwin` (native), `js` (web), `apache5` (desktop).
- **Expect/Actual:** Use sparingly. Prefer interfaces in `commonMain`, platform-specific impls via DI.

## 10. Coding Guidelines

### Naming
- **Composables:** PascalCase. Root composables end in `Root`.
- **ViewModels:** `FeatureViewModel`.
- **DI Modules:** `<Feature><Layer>Module` classes (e.g. `AuthenticationDataModule`, `AuthPresentationModule`).

### Patterns
- **Error Handling:** Always use `Result<D, E>` (`Success`/`Failure`, NOT `Error`). Convert `DataError` to `UiText` via `toUiText()` (`core/presentation/.../util/DataErrorToUiText.kt`).
- **Async Work:** `viewModelScope.launch` in ViewModels. `Dispatchers.IO` for heavy/blocking calls (Ktor/Room non-blocking).
- **Immutability:** Prefer `val` and `data class` with `copy()`.

### Testing
- **Stack:** kotlin-test (`@BeforeTest`, `assertEquals`, `assertIs`) + Turbine (`flow.test { }`) + hand-written fakes. **No mocking library.**
- Shared fakes in `:features:<name>:testing` modules; screen-local fakes next to the test.
- ViewModel tests: `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest`, `resetMain()` in `@AfterTest`, `runTest(testDispatcher)`. After editing a `TextFieldState`: `Snapshot.sendApplyNotifications()` + `advanceUntilIdle()`.
- Tests live in `commonTest`; Room/repo tests may live in `desktopTest` (e.g. `OfflineFirstSyncRepositoryTest`).
- Target unit tests for domain logic and ViewModels.

---

## 11. Critical Workflows
- **Format:** `./gradlew ktlintFormat`. CI runs `ktlintCheck :build-logic:convention:ktlintCheck`.
- **Fast verify:** compile only touched modules, e.g. `./gradlew :features:tabgroup:domain:compileKotlinJvm`. Full Android build: `./gradlew :androidApp:assembleDebug`.
- **Tests:** `./gradlew allTests` (all targets) or narrower, e.g. `:androidApp:testDebugUnitTest`.
- **Compiler warnings:** CI checks build log against `.github/compiler-warnings-baseline.txt` via `.github/check-compiler-warnings.sh` — new warnings fail the PR pipeline. Don't introduce any.
- **CI parity:** `.github/workflows/pr_pipeline.yml` = ktlint + `:androidApp:assembleDebug lintDebug testDebugUnitTest` + `:composeApp:desktopJar` + wasm distribution + `allTests`.
- **Local Config:** `local.properties` must have `API_KEY`. `CLIENT_BUILD_TOKEN` is optional (see README) — once the backend enables its version gate, native builds without a matching one get `426`.
- **Sync:** `./gradlew help` (triggers sync).