# TabMates AI Agent Guide

This document provides a comprehensive overview of the TabMates project architecture, patterns, and guidelines to help AI agents understand and contribute to the codebase without exhaustive file analysis.

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

The project is modularized by feature and layer. Use **typesafe project accessors** (e.g., `projects.core.domain`).

### Core Modules (`:core:*`)
- **`:core:domain`**: Pure Kotlin. Business models, standard `Result<D, E>` type, `Error` interfaces, and global loggers.
- **`:core:data`**: Shared networking setup (`HttpClientFactory`), standard Ktor configurations, and common data sources.
- **`:core:presentation`**: Shared UI logic, `UiText` for localized strings, `ObserveAsEvents` for one-time events.
- **`:core:designsystem`**: Shared Compose tokens (Color, Type, Shape) and reusable atomic components (Buttons, TextFields, etc.).

### Feature Modules (`:features:*:*`)
Each feature is split into:
- **`:domain`**: Interfaces (`Service`, `Repository`), Business models, and Validators.
- **`:data`**: Implementations of domain interfaces, DTOs, Mappers, and API services.
- **`:presentation`**: Compose UI (Screens, Components), ViewModels, and Navigation Routes.
- **`:database`** (Optional): Room entity definitions and DAOs.

### Application Modules
- **`:composeApp`**: The main entry point for shared UI. Aggregates all features and wires Navigation/DI.
- **`:androidApp`**: Android-specific configuration, depends only on `:composeApp`.

---

## 3. Architecture Layers

### Domain Layer (Pure Kotlin)
- Define `interface AuthService` or `interface GroupRepository`.
- Use `de.tabmates.core.domain.util.Result<D, E>` for all operation outcomes.
- Models should be data classes, ideally immutable.

### Data Layer
- Implement domain interfaces.
- Use `Ktor` for networking.
- Use `Mappers` to convert DTOs (Data Transfer Objects) to Domain Models.
- **Convention:** DTOs suffix with `Request` or `Response`.

### Presentation Layer (Compose Multiplatform)
- **ViewModels:**
    - Use `StateFlow` for UI state (e.g., `state: StateFlow<LoginState>`).
    - Use `Channel` and `Flow` for one-time events (e.g., `events: Flow<LoginEvent>`).
    - Inherit from `androidx.lifecycle.ViewModel`.
- **UI Components:**
    - `Root` composables (e.g., `LoginRoot`) handle ViewModel interaction and event observation.
    - Screen composables (e.g., `LoginScreen`) are stateless and take data/callbacks.
    - Use `ObserveAsEvents` to handle ViewModel events (Snackbars, Navigation).

---

## 4. Navigation (Navigation 3)

- **Routes:** Defined as `@Serializable` data classes/objects in the feature's `presentation` module (e.g., `data object Home : NavKey`).
- **Graphs:** Features define an `EntryProviderScope<NavKey>.featureGraph` extension.
- **Wiring:** All feature graphs are aggregated in `composeApp/App.kt` using `NavDisplay`.
- **Top-level Tabs:** Implement `TopLevelTab` and `LoggedIn` interfaces for consistent bottom bar behavior.

---

## 5. Dependency Injection (Koin)

- Each module defines a `val moduleName = module { ... }`.
- Feature modules are aggregated in `composeApp/src/commonMain/kotlin/de/tabmates/composeapp/App.kt`.
- Use `koinViewModel()` in Composables.

---

## 6. Design System & Theming

- **Theme:** `TabMatesTheme` (built on Material3).
- **Tokens:** Found in `:core:designsystem`. Use `MaterialTheme.colorScheme` or custom `TabMatesTheme` properties.
- **Resources:** Use `Res.string.key` or `Res.drawable.key` via Compose Resources.
- **Localization:** Managed in `composeResources/values/strings.xml` within each module.

---

## 7. Compose Previews

Use multi-preview annotations from `:core:designsystem` to ensure consistent testing across themes and devices.

### Multi-preview Annotations
- **`@PreviewThemes`**: Generates Light and Dark mode previews. **Preferred for most components.**
- **`@PreviewPhones`**: Generates Portrait and Landscape previews for phones.
- **`@PreviewScreenSizes`**: Generates previews for Phone, Foldable, Tablet, Desktop, and Web.
- **`@PreviewAll`**: Generates every combination of theme and screen size (14 previews). Use sparingly.

### Pattern
Always wrap the previewed component in `TabMatesTheme` and a `Surface` (if needed for background).
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

**NEVER** configure KMP manually. Use the following plugins:
- `tabmates.convention.kmp.library`: Standard KMP library.
- `tabmates.convention.cmp.library`: CMP library with Compose dependencies.
- `tabmates.convention.cmp.feature`: Feature presentation module (includes VM, Lifecycle, Core Presentation).
- `tabmates.convention.room`: Enables Room with KSP.
- `tabmates.convention.buildkonfig`: For BuildConfig-like constants.

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
- **Expect/Actual:** Use sparingly. Prefer defining interfaces in `commonMain` and providing platform-specific implementations via DI.

## 10. Coding Guidelines

### Naming
- **Composables:** PascalCase. Root composables end in `Root`.
- **ViewModels:** `FeatureViewModel`.
- **DI Modules:** `featureDataModule`, `featurePresentationModule`.

### Patterns
- **Error Handling:** Always use the `Result` type. Convert `DataError` to `UiText` using `asUiText()` extension.
- **Asynchronous Work:** Use `viewModelScope.launch` in ViewModels. Use `Dispatchers.IO` for heavy computations or blocking calls (though Ktor/Room are non-blocking).
- **Immutability:** Prefer `val` and `data class` with `copy()`.

### Testing
- **commonMain:** Use **Mokkery** for mocking interfaces.
- **androidMain / jvmMain:** Use **Mockk**.
- Target unit tests for Domain logic and ViewModels.

---

## 11. Critical Workflows
- **Sync:** `./gradlew help` (triggers sync).
- **Format:** `./gradlew ktlintFormat`.
- **Build:** `./gradlew assembleDebug`.
- **Local Config:** `local.properties` must have `API_KEY`.
