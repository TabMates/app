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

Browsers cannot set headers on a WebSocket handshake, so `WebSocketTransport` also sends the
credentials as `access_token`/`api_key` query parameters, which the server accepts on `/ws/group`
only (native clients keep using the headers). The token can therefore appear in reverse-proxy
access logs — redact query strings for `/ws/group` there.

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

The `API_KEY` is compiled into the JS bundle, so on web it is **public** — treat it as a client
identifier, not a secret. Real authorization is the JWT; the api-key only shields the public
auth/register endpoints, so no endpoint may rely on it alone.
