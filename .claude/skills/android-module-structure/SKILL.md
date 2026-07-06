---
name: android-module-structure
description: |
  TabMates KMP/CMP module layout, dependency rules, Gradle convention plugins, source-set hierarchy, and Koin Annotations DI wiring. Use this skill whenever adding a module, deciding where code lives, creating a feature, configuring Gradle/convention plugins, or setting up dependency injection. Trigger on phrases like "add a module", "create a feature", "project structure", "convention plugin", "build-logic", "where does X live", "Koin", "DI module", "inject", or "@Single".
---

# TabMates Module Structure & DI

## Core Philosophy

- Split by feature first, then by layer. Clean Architecture: `presentation` → `domain` ← `data`; domain depends on nothing.
- Code lives in a feature module unless needed by 2+ features — then it moves to the matching `core` submodule.
- Features never depend on each other. Cross-feature sharing goes through `core:*`.
- Use **typesafe project accessors** (`projects.core.domain`), never string paths.

## Module Layout (real tree — see `settings.gradle.kts`)

```
:androidApp                          ← Android app shell, depends only on :composeApp
:composeApp                          ← shared-UI entry point; wires NavDisplay + Koin
:build-logic                         ← convention plugins (included build)
:core:domain                         ← pure Kotlin: models, Result, Error, DataError, loggers
:core:data                           ← HttpClientFactory, Ktor helpers (HttpClientExt.kt), shared DTOs/mappers
:core:presentation                   ← UiText, ObserveAsEvents, navigation contracts (TopLevelTab, ScreenWithTopBar, …)
:core:designsystem                   ← TabMatesTheme, tokens, atomic components, preview annotations
:features:<name>:domain              ← interfaces (Service/Repository), models, validators
:features:<name>:data                ← impls, DTOs, mappers, DI module
:features:<name>:presentation        ← ViewModels, screens, NavKeys, feature graph
:features:<name>:database   (opt)    ← Room DB/entities/DAOs/migrations (tabgroup only)
:features:<name>:testing    (opt)    ← shared fakes (authentication, notifications)
```

Features: `appupdate` (domain+data only), `authentication`, `notifications`, `tabgroup` (+ `sqliteWasmWorker` for web SQLite). Not every feature needs every layer.

## Dependency Rules

| Layer | May depend on |
|---|---|
| `presentation` | own `domain`, `core:domain`, `core:presentation`, `core:designsystem` |
| `data` | own `domain` (+ own `database`), `core:domain`, `core:data` |
| `domain` | `core:domain` only |
| `:composeApp` | everything (aggregates graphs + DI) |
| `:androidApp` | `:composeApp` only |

## Convention Plugins (`build-logic`)

**NEVER configure KMP/Android manually.** IDs prefixed `de.tabmates.convention.` — registrations in `build-logic/convention/build.gradle.kts`:

| Plugin | Purpose |
|---|---|
| `kmp.library` | standard KMP library (domain/data) |
| `cmp.library` | CMP library with Compose deps |
| `cmp.feature` | feature presentation module (VM, Lifecycle, core presentation bundled) |
| `cmp.application` | `:composeApp` |
| `cmp.resources` | Compose Resources generation |
| `android.application` / `android.application.compose` | `:androidApp` |
| `room` | Room + KSP |
| `koin` | Koin Annotations + KSP compiler |
| `ktlint` | ktlint checks |
| `buildkonfig` | `BuildKonfig` constants |

All versions via `gradle/libs.versions.toml` — no hardcoded versions.

## Source-Set Hierarchy (`build-logic` `HierarchyTemplate.kt`)

```
common ─ mobile (android + ios) │ web (wasmJs) │ native (ios + macos, apple) │ desktop (jvm)
```
Ktor engines: `okhttp` (android), `darwin` (native), `js` (web), `apache5` (desktop). Prefer commonMain interfaces + platform impls via DI over expect/actual.

## Dependency Injection (Koin Annotations + KSP — no DSL)

Do NOT write `module { }` / `singleOf` / `viewModelOf`. Pattern:

```kotlin
// features/<name>/<layer>/.../di/<Feature><Layer>Module.kt
@Module
@Configuration
@ComponentScan("de.tabmates.features.authentication.data")
class AuthenticationDataModule
```

- Bindings: `@Single` on impls (`@Single(binds = [AuthService::class])` when needed), `@KoinViewModel` on ViewModels, `@Single(createdAtStart = true)` for eager singletons.
- Platform-specific: `expect class PlatformCoreDataModule()` in commonMain, `actual` per source set (`core/data/src/*Main/.../di/`).
- Assembly: `@KoinApplication class TabMatesKoinApp` (`composeApp/.../di/AppModule.kt`), started in `App()` with `KoinApplication(configuration = koinConfiguration<TabMatesKoinApp>())`. The Koin compiler plugin auto-aggregates all `@Configuration` modules — no manual `modules(...)` list.
- In Root composables: `koinViewModel()`.

## Key Libraries

| Concern | Library |
|---|---|
| DI | Koin Annotations (KSP) |
| Networking | Ktor Client |
| DB | Room (KMP) |
| Navigation | Navigation 3 (`NavKey`/`NavDisplay`) |
| Serialization | KotlinX Serialization |
| Strings/images | Compose Resources (`Res.string.*`) |
| Logging | Kermit |
| Testing | kotlin-test + Turbine + fakes |
| Secrets | `local.properties` → `BuildKonfig` |

## Checklist: New Feature Module

- [ ] Create `:features:<name>:domain|data|presentation` (+ `database`/`testing` if needed); add each to `settings.gradle.kts`
- [ ] Apply convention plugins (`kmp.library` for domain/data, `cmp.feature` for presentation, `koin` where DI needed)
- [ ] Add `@Module @Configuration @ComponentScan` class per layer (auto-aggregated by the Koin compiler)
- [ ] Register feature graph + `SerializersModule` in `composeApp/App.kt` (see android-navigation skill)
- [ ] No cross-feature dependencies; shared logic → `core:*`
