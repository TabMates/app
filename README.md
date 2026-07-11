# TabMates

Split shared expenses with friends, flatmates and travel groups — across Android, iOS, Desktop and Web from a single Kotlin codebase.

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11-4285F4?logo=jetpackcompose&logoColor=white)
![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-lightgrey)
![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)

TabMates is a Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) app for tracking who paid for what in a group and settling up. The UI, business logic and data layer are shared across all targets; only thin platform shells differ.

## Features

- **Groups** — create groups, invite members via share links, join by link.
- **Expenses** — add, edit and view expenses with flexible splitting and **multiple currencies per expense**.
- **Offline-first** — browse groups and expenses and keep working without a connection; changes sync once you're back online (local Room cache).
- **Settle up** — see balances and who owes whom.
- **Activity feed** — recent changes across your groups.
- **Accounts** — register, log in, continue as guest, email verification and password reset.
- **Push notifications** — group activity via Firebase Cloud Messaging (Android/iOS), a WebSocket-driven channel on Desktop, and the Firebase JS SDK on Web. See [`features/notifications/README.md`](features/notifications/README.md).
- **Settings** — theme (light/dark/system), in-app language, notification toggle with permission handling, and an open-source licenses screen.

## Platforms

| Target | Shell |
|--------|-------|
| Android | `:androidApp` |
| iOS | `iosApp` (SwiftUI host) |
| Desktop (JVM) | `:composeApp` desktop entry |
| Web (WasmJS) | `:composeApp` web entry |

## Tech stack

- **Kotlin Multiplatform** + **Compose Multiplatform** UI
- **Clean Architecture** + **MVI/MVVM** presentation
- **Koin** (annotations + compiler) for dependency injection
- **Navigation 3** (type-safe routes)
- **Ktor** client (ContentNegotiation + WebSockets) for networking
- **Room** (KMP) for local persistence
- **kotlinx.serialization**, **KSafe** (encrypted storage), **BuildKonfig** (build-time config)
- **kmpnotifier** / Firebase for push, **AboutLibraries** for license attribution
- Gradle **convention plugins** in `build-logic`, version catalog in `gradle/libs.versions.toml`

## Architecture

The project is modularized **by feature and by layer**. Dependencies point inward: `presentation → domain ← data`, with `core` shared by all features.

- **`:core:domain`** — pure Kotlin: models, `Result<D, E>`, error types, logging.
- **`:core:data`** — shared networking (`HttpClientFactory`), encrypted storage (`SecureStore`), preferences.
- **`:core:presentation`** — shared UI utilities (`UiText`, `ObserveAsEvents`, navigation contracts).
- **`:core:designsystem`** — theme, tokens and reusable Compose components.
- **`:features:*`** — each feature split into `domain` (interfaces, models), `data` (implementations, DTOs, mappers), `presentation` (screens, ViewModels, routes), optional `database`/`testing`.
- **`:composeApp`** — shared entry point: wires navigation and DI, hosts Desktop/Web `main` and the iOS `MainViewController`.
- **`:androidApp`** — Android application shell.

See [`AGENTS.md`](AGENTS.md) for detailed conventions.

## Project structure

```
composeApp/                 Shared app entry (DI + navigation), desktop & web main, iOS controller
androidApp/                 Android application
iosApp/                     iOS SwiftUI host (Xcode project)
core/
  data/  domain/  presentation/  designsystem/
features/
  authentication/  data · domain · presentation · testing
  notifications/   data · domain · testing
  tabgroup/        data · domain · presentation · database · sqliteWasmWorker
build-logic/                Gradle convention plugins
gradle/libs.versions.toml   Version catalog
```

## Getting started

### Prerequisites

- JDK 17+
- Android Studio (latest stable) / IntelliJ IDEA
- Xcode (for iOS), on macOS

### Configuration

Build-time config is injected via BuildKonfig and **required** to build. Add these to a `local.properties` file in the repo root (git-ignored), or provide them as environment variables in CI:

```properties
API_KEY=your-api-key
BASE_URL_HTTP=https://your-backend.example.com
BASE_URL_WS=wss://your-backend.example.com
```

Optional per-target overrides let every target run against a local backend at the same time.
The Android emulator reaches the host via `10.0.2.2`, and the browser must stay same-origin
(the backend has no CORS config), so `composeApp/webpack.config.d/proxy.js` forwards `/api`
and `/ws` from the dev server to the backend. iOS simulator and desktop use the defaults above
(`http://localhost:8080` locally) directly:

```properties
BASE_URL_HTTP_ANDROID=http://10.0.2.2:8080
BASE_URL_WS_ANDROID=ws://10.0.2.2:8080/ws
BASE_URL_HTTP_WEB=http://localhost:8081
BASE_URL_WS_WEB=ws://localhost:8081/ws
```

The proxy target follows the active `BASE_URL_HTTP`, so switching it (e.g. to
`https://dev.tabmates.de`) points the web dev build at that environment too — keep
`BASE_URL_HTTP_WEB` on the dev server and restart `wasmJsBrowserDevelopmentRun`. The web app
must always go through the proxy; the backend serves no CORS headers, so the browser cannot
call it cross-origin directly.

Push notifications additionally need Firebase config — see [`features/notifications/README.md`](features/notifications/README.md). A dummy `androidApp/google-services.json` is committed so the project builds out of the box; replace it with a real one for working push.

### Run

```bash
# Android — install on a device/emulator (or just run :androidApp from the IDE)
./gradlew :androidApp:installDebug

# Desktop (JVM), hot-reload enabled
./gradlew :composeApp:hotRunDesktop

# Web (WasmJS) — dev server on http://localhost:8081 (8080 is the backend dev server)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run, or use the KMP/AndroidStudio run configuration.

### Web hosting requirements

The web app stores its Room database in the browser's Origin Private File System, which
requires a cross-origin-isolated context. Whatever serves the production web build must send
these headers on **every** response (the dev server already does, see
`composeApp/webpack.config.d/headers.js`):

```
Cross-Origin-Opener-Policy: same-origin
Cross-Origin-Embedder-Policy: require-corp
```

Without them `crossOriginIsolated` is false, the SQLite worker cannot initialize OPFS, and the
app runs without local persistence. HTTPS is also required (secure context). The Firebase
scripts loaded from gstatic.com are compatible with these headers (verified: they are served
with `Cross-Origin-Resource-Policy: cross-origin`, so `require-corp` is fine — if a future
gstatic change drops that header, switch the production COEP to `credentialless` or self-host
the scripts); any *new* cross-origin `<script>`/`<img>`/font added to `index.html` must likewise
send CORP or be loaded with CORS, or the browser will block it under COEP.

On hosts that cannot set response headers (GitHub Pages), the vendored
`composeApp/src/wasmJsMain/resources/coi-serviceworker.js` (first script in `index.html`)
provides the same isolation: it registers a root-scope service worker that injects the headers
into every response, at the cost of one automatic reload on first visit. It prefers COEP
`credentialless` and degrades to `require-corp` automatically; both yield `crossOriginIsolated`.
It no-ops when the server already sends the headers, so dev is unaffected. Because it owns the
root service-worker scope, the Firebase messaging worker is registered under a dedicated
sub-scope in `firebase-init.js` — never register another service worker without an explicit
non-root scope, or COOP/COEP injection silently dies on the next reload.

#### Content-Security-Policy

The web session/tokens live in `localStorage` (KSafe), which any script on the origin can read,
so a CSP is the main defense against token exfiltration via XSS. Serve it as an **HTTP header**
when the host allows it; on GitHub Pages the deploy workflow injects it as a `<meta>` tag into
the built `index.html` instead (`.github/workflows/deploy-web.yml`), with `connect-src` derived
from the build's API/WS origins. A `<meta>` CSP ignores `frame-ancestors` by spec, so
clickjacking protection is unavailable on header-less hosts — everything else below applies.
The tag is not in the source `index.html` on purpose: the dev server talks plain `ws://`
same-origin, which `'self'` does not reliably cover. Baseline:

```
default-src 'self';
script-src 'self' 'wasm-unsafe-eval' https://www.gstatic.com;   # wasm-unsafe-eval is REQUIRED for Compose/WasmJS
worker-src 'self' blob:;                                        # the SQLite web worker
connect-src 'self' https://<api-host> wss://<api-host>;         # this environment's API http + ws origins
img-src 'self' data:;
style-src 'self' 'unsafe-inline';
frame-ancestors 'none';
```

Omitting `wasm-unsafe-eval` prevents the WasmJS bundle from loading at all; omitting the API
origin from `connect-src` breaks every API/websocket call. Verify no CSP violations appear in
the console after deploying.

#### CORS, websocket origin & the API key in production

The hosted web app calls the backend **cross-origin**, so its origin must be listed in **both**
server settings: `TABMATES_CORS_ALLOWED_ORIGINS` (HTTP) and — because the websocket handshake
carries the page origin — the websocket allowed-origin list (production reuses
`TABMATES_CORS_ALLOWED_ORIGINS` for this). Missing either one fails silently: HTTP is blocked by
CORS, or the `wss` upgrade is rejected 403.

Local dev never exercises this — it goes same-origin through the webpack proxy — so **smoke-test
from a real cross-origin build before launch**: serve the web app from an origin different from
the API and confirm the preflight `OPTIONS` returns the `Access-Control-Allow-Origin` header, an
authenticated request succeeds, and the `wss` `/ws/group` upgrade connects. The production web
build must set `BASE_URL_HTTP_WEB` / `BASE_URL_WS_WEB` to the real API origin (there is no proxy
in production).

Browsers cannot set headers on a WebSocket handshake, so `WebSocketTransport` also sends the
credentials as `access_token`/`api_key` query parameters, which the server accepts on `/ws/group`
only (native clients keep using the headers). The token can therefore appear in reverse-proxy
access logs — redact query strings for `/ws/group` there.

#### Deploying to GitHub Pages

`.github/workflows/deploy-web.yml` builds `:composeApp:wasmJsBrowserDistribution` on every tag
push (and manually via *Run workflow*), injects the CSP meta tag and `CNAME`, and publishes to
GitHub Pages at `app.tabmates.de`. One-time setup:

- **Secrets**: `BASE_URL_HTTP_WEB` / `BASE_URL_WS_WEB` → the target backend
  (e.g. `https://<api-host>` / `wss://<api-host>/ws`); `API_KEY`, `BASE_URL_HTTP`,
  `BASE_URL_WS` are reused from the Android release setup.
- **Pages**: repo Settings → Pages → Source: *GitHub Actions*; Custom domain: `app.tabmates.de`;
  enable *Enforce HTTPS* once the certificate is issued.
- **DNS**: `app` `CNAME` → `<github-org>.github.io.` (lowercase).
- **Backend**: add `https://app.tabmates.de` to `TABMATES_CORS_ALLOWED_ORIGINS` on the target
  environment and restart it.

The `API_KEY` is compiled into the JS bundle, so on web it is **public** — treat it as a client
identifier, not a secret. Real authorization is the JWT; the api-key only shields the public
auth/register endpoints, so no endpoint may rely on it alone.

### Build & test

```bash
./gradlew build          # build all targets
./gradlew check          # run tests + lint (ktlint)
./gradlew ktlintFormat   # auto-format
```

## License

Licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE).
