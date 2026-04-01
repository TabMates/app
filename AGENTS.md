# TabMates AI Agent Guide

## Project Overview

**Kotlin Multiplatform (KMP) + Compose Multiplatform** app using **Clean Architecture (MVVM)** with modularization by feature.
**Targets:** Android, iOS, Desktop (JVM), Web (JS/WasmJS).
**Package root:** `de.tabmates`.

## Module Structure

| Module path | Plugin to use | Purpose |
|---|---|---|
| `composeApp` | `cmp.application` | Shared UI entry point, aggregates all feature modules |
| `androidApp` | `android.application.compose` | Android entry point, depends only on `:composeApp` |
| `core/domain` | `kmp.library` | `Result<D,E>`, `DataError`, `Error` interface, `TabMatesLogger` |
| `core/data` | `kmp.library` + `buildkonfig` | `HttpClientFactory`, Ktor extensions, platform HTTP engines |
| `core/presentation` | `cmp.library` | `UiText`, `ObserveAsEvents` utility |
| `core/designsystem` | `cmp.library` | Shared Compose design tokens / components |
| `features/*/domain` | `kmp.library` | UseCases, Repository interfaces |
| `features/*/data` | `kmp.library` | Repository implementations |
| `features/*/presentation` | `cmp.feature` | Screens, ViewModels (auto-gets `:core:presentation`, `:core:designsystem`, Koin, Lifecycle) |
| `features/*/database` | `kmp.library` + `room` | Room DB (only `tabgroup` has this currently) |

## Convention Plugins (build-logic)

Always use convention plugins — never configure KMP/Compose manually:
- **`alias(libs.plugins.tabmates.convention.kmp.library)`** — KMP + Android + iOS + Desktop + Web targets, ktlint, serialization. Auto-derives `namespace` and package from module path (e.g., `:features:authentication:data` → `de.tabmates.features.authentication.data`).
- **`alias(libs.plugins.tabmates.convention.cmp.feature)`** — Extends `cmp.library` + adds Koin BOM, Koin Compose, ViewModel, Lifecycle, SavedState, `:core:presentation`, `:core:designsystem`.
- **`alias(libs.plugins.tabmates.convention.cmp.library)`** — Extends `kmp.library` + Compose compiler/runtime, Material3, Foundation, Icons.
- **`alias(libs.plugins.tabmates.convention.room)`** — KSP + Room 3, schemas in `$projectDir/schemas`. Runs KSP for Android, iOS, Desktop, Web.
- **`alias(libs.plugins.tabmates.convention.buildkonfig)`** — Generates `BuildKonfig` with `API_KEY` (from `local.properties` or env) and `IS_DEBUG`.

## Source Set Hierarchy

Custom hierarchy template in `build-logic` (`HierarchyTemplate.kt`):
```
common
├── mobile  (android + ios)
├── web     (js + wasmJs)
├── native  (ios + macos)
│   └── apple → ios, macos
└── desktop (jvm)
```
**Ktor engines by source set:** `okhttp` (androidMain), `darwin` (nativeMain), `js` (webMain), `apache5` (desktopMain).

### Navigation
Use **Navigation 3 (nav3)** (`org.jetbrains.androidx.navigation3:navigation3-ui`) for screen navigation across all platforms.
Each feature module defines its own routes and does **not** reference routes from other feature modules directly. Cross-feature navigation is wired at the `composeApp` level.

### Dependency References
Use **typesafe project accessors**: `implementation(projects.core.domain)` (not `project(":core:domain")`).
Use **version catalog bundles** where available: `libs.bundles.ktor.common`, `libs.bundles.koin.common`.

## Critical Workflows

- **Build:** `./gradlew build`
- **Lint:** ktlint runs automatically via `kmp.library` plugin. Fix with `./gradlew ktlintFormat`.
- **Run Android:** `./gradlew :androidApp:installDebug`
- **Run iOS:** Open `iosApp/iosApp.xcodeproj` in Xcode.
- **Required local config:** `local.properties` must contain `API_KEY="..."` (or set `API_KEY` env var). Build will fail without it.
- **⛔ Never commit `local.properties` or any API keys / secrets to version control.**

## Testing Strategy

- **Preference:** Unit tests are preferred over integration/UI tests.
- **Mocking:**
    - **Common Code (`commonMain`):** Use **Mokkery**.
    - **Platform Specific (`androidMain`, `jvm`):** Use **Mockk**.
