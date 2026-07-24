# Web deployment

Everything needed to host the TabMates web (WasmJS) build in production: cross-origin
isolation headers, Content-Security-Policy, CORS / websocket origin configuration, and the
GitHub Pages deploy workflow. Local development is unaffected by most of this — the webpack
dev server already sends the right headers and proxies the backend same-origin.

## Web hosting requirements

The web app stores its Room database in the browser's Origin Private File System, which
requires a cross-origin-isolated context. Whatever serves the production web build must send
these headers on **every** response (the dev server already does, see
`composeApp/webpack.config.d/headers.js`):

```
Cross-Origin-Opener-Policy: same-origin
Cross-Origin-Embedder-Policy: credentialless
```

Without them `crossOriginIsolated` is false, the SQLite worker cannot initialize OPFS, and the
app runs without local persistence. HTTPS is also required (secure context). COEP is
`credentialless` rather than `require-corp` because the Cloudflare **Turnstile** widget (an iframe
from `challenges.cloudflare.com`, used for the auth bot-check) fails under `require-corp`;
`credentialless` still yields `crossOriginIsolated`, so OPFS/SQLite is unaffected. Under
`credentialless` a cross-origin no-cors subresource (the gstatic Firebase scripts) loads without
credentials and needs no CORP header; a *new* cross-origin `<script>`/`<img>`/font that must send
credentials still needs CORS or CORP, or the browser will block it.

On hosts that cannot set response headers (GitHub Pages), the vendored
`composeApp/src/wasmJsMain/resources/coi-serviceworker.js` (first script in `index.html`)
provides the same isolation: it registers a root-scope service worker that injects the headers
into every response, at the cost of one automatic reload on first visit. It prefers COEP
`credentialless` and degrades to `require-corp` automatically; both yield `crossOriginIsolated`.
It no-ops when the server already sends the headers, so dev is unaffected. Because it owns the
root service-worker scope, the Firebase messaging worker is registered under a dedicated
sub-scope in `firebase-init.js` — never register another service worker without an explicit
non-root scope, or COOP/COEP injection silently dies on the next reload.

## PWA: installability & offline app shell

The web build is an installable, offline-capable PWA (Add to Home Screen / Install on
Android, iOS and desktop). Two pieces provide this, both shipped as static resources under
`composeApp/src/wasmJsMain/resources/` (auto-bundled into the dist, no workflow change):

- **Web app manifest** — `manifest.webmanifest` (linked from `index.html`, plus `theme-color`,
  `apple-touch-icon` and the `apple-mobile-web-app-*` tags iOS needs since it ignores the
  manifest). Icons live in `icons/` (192/512 `any`, a 512 `maskable` on the brand background, a
  180 apple-touch-icon, a 32 favicon). `theme_color` is the brand primary `#b05530`,
  `background_color` `#fffbff`. The manifest and its same-origin PNG icons satisfy the deploy CSP
  (`default-src`/`img-src 'self'`) as-is. `.webmanifest` is served `application/manifest+json` by
  Pages; if a host serves it wrong, rename to `manifest.json`.
- **Offline app-shell cache** — folded **into `coi-serviceworker.js`**, not a second worker,
  because the root scope must stay with the COOP/COEP injector (see above). Its `fetch` handler
  caches same-origin GETs (stale-while-revalidate for assets, network-first with a cached-`/`
  fallback for navigations) and passes **every** served response — network or cache — through
  `withCoiHeaders()`, so cross-origin isolation (and therefore OPFS/SQLite) still holds on an
  offline launch. Cross-origin requests (gstatic Firebase) and non-GETs keep the original
  network-only path and are never cached. Updates are silent: content-hashed bundle filenames plus
  `skipWaiting()`/`clients.claim()` mean a new deploy is picked up on the next launch. Bump
  `SHELL_CACHE` (`tabmates-shell-vN`) to force-invalidate the shell cache; the `activate` handler
  deletes older `tabmates-shell-*` caches.

Because the offline data layer (Room in OPFS, durable outbox, delta + reconnect sync) is already
shared `commonMain` code, once a user has loaded and logged in online the installed PWA works
offline and syncs on reconnect — the same behaviour as Android. Firebase push is the one thing
that needs the network: `firebase-init.js` (same-origin, cached) guards the missing cross-origin
SDK and installs no-op glue so an offline launch still boots; push resumes on the next online run.

Verify after a build (`:composeApp:wasmJsBrowserDistribution`, served over HTTPS/localhost):
DevTools → Application → Manifest is installable with no errors, the service worker is
*activated and controlling*, `crossOriginIsolated === true`, and with Network → Offline a reload
still renders the app shell.

## Content-Security-Policy

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
script-src 'self' 'wasm-unsafe-eval' https://www.gstatic.com https://challenges.cloudflare.com;   # wasm-unsafe-eval is REQUIRED for Compose/WasmJS; cloudflare = Turnstile api.js
frame-src https://challenges.cloudflare.com;                    # the Turnstile challenge iframe
worker-src 'self' blob:;                                        # the SQLite web worker
connect-src 'self' https://<api-host> wss://<api-host>;         # this environment's API http + ws origins
img-src 'self' data:;
style-src 'self' 'unsafe-inline';
frame-ancestors 'none';
```

There is no `frame-src` fallback to `'self'` here — without an explicit `frame-src` it falls back to
`default-src 'self'`, which would block the Turnstile iframe even after the COEP fix.

Omitting `wasm-unsafe-eval` prevents the WasmJS bundle from loading at all; omitting the API
origin from `connect-src` breaks every API/websocket call. Verify no CSP violations appear in
the console after deploying.

## CORS, websocket origin & the API key in production

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

Browsers cannot set headers on a WebSocket handshake, so `WebSocketTransport` sends the JWT as an
`access_token` query parameter, which the server accepts on `/ws/group` only. On **web** that is the
only query param (no `api_key`; the server recognizes the browser by its allow-listed origin);
**native** clients additionally send the `api_key` query param and the header pair. The token can
therefore appear in reverse-proxy access logs — redact query strings for `/ws/group` there.

## Deploying to GitHub Pages

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

The web bundle **no longer ships `x-api-key`** (the wasmJs BuildKonfig nulls it, so neither the REST
header nor the `/ws/group` `api_key` param is sent). That key was never secret in a browser
(readable in DevTools, replayable), so the server now recognizes browser traffic by its allow-listed
`Origin` instead, and gates the abuse-prone unauthenticated auth endpoints with a Cloudflare
Turnstile bot-check (`cf-turnstile-response`, from an invisible widget — see the Turnstile CSP/COEP
notes above). Real authorization is still the per-user JWT. **Native** clients keep sending the
real api-key and are Turnstile-exempt. The web `API_KEY` CI secret is left in place for now
(harmless, unused by web) until native no longer shares the pipeline expectation.

## Deep links & Android App Links

One URL — `https://app.tabmates.de/<path>` — is meant to open the **installed app** (Android
App Links) when present, and otherwise load the **web client** at the same URL, with no
"Open in app?" interstitial. Both platforms resolve the URL through the same shared code
(`composeApp/src/commonMain/.../deeplink/`), so there is no per-platform link logic.

Key point: the user-facing link host is **decoupled from the backend API host**. Links and
deep-link matching use `BASE_URL_PUBLIC` (e.g. `https://app.tabmates.de`), *not* the API
`BASE_URL_HTTP`. `BASE_URL_PUBLIC` is **required to build** (like `BASE_URL_HTTP`); for local
dev set it to your `BASE_URL_HTTP` value. Currently deep-linkable: `/api/auth/verify`,
`/api/auth/reset-password`, `/j/<token>` (invites). `/groups/<id>` resolves in-app from
notifications but is intentionally **not** an external App Link.

**Web fallback (app not installed).** GitHub Pages is static with no SPA rewrite, so a direct
hit to a sub-path (`/j/<token>`) would 404. `composeApp/src/wasmJsMain/resources/404.html`
stashes the original URL in `sessionStorage` and redirects to the root (a real file, so it is
reload-safe with coi-serviceworker); `webMain/.../main.kt` then consumes the stashed URL and
feeds the shared `DeepLinkHandler`. The address bar stays on `/` — consistent with the app,
which never syncs the URL to in-app navigation.

**Android App Links verification.** The manifest sets `autoVerify="true"` for
`https://app.tabmates.de/…` (the host is hardcoded in `AndroidManifest.xml` and must match the
prod `BASE_URL_PUBLIC` host; the lint check `AppLinkUrlError` rejects a placeholder host).
Verification requires
`https://app.tabmates.de/.well-known/assetlinks.json` to be reachable — it ships as a static
resource (`composeApp/src/wasmJsMain/resources/.well-known/assetlinks.json`) and is served with
`application/json` by Pages (`.json` extension). **Fill in the SHA-256** placeholder with the
**Play App Signing** certificate fingerprint from Play Console → *App integrity → App signing
key certificate* (it is public, not a secret). After deploying, confirm:

```
curl -sI https://app.tabmates.de/.well-known/assetlinks.json   # 200, content-type: application/json
adb shell pm verify-app-links --re-verify de.tabmates.androidapp
adb shell pm get-app-links de.tabmates.androidapp              # expect: verified
```

Debug builds are signed with the debug key, which is **not** in `assetlinks.json`, so they will
not auto-verify — test the installed-app path with a release build (or the `tabmates://` custom
scheme). For manual testing:
`adb shell am start -a android.intent.action.VIEW -d "https://app.tabmates.de/j/TESTTOKEN"`.

**Backend (separate repo) — required for auth email links.** The emailed
`/api/auth/verify` and `/api/auth/reset-password` links (and group-notification deep links)
must be emitted on the **public host** (`app.tabmates.de`) so they match the app's registered
links and open the app/web rather than hitting the API host directly.
