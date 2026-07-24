/*! coi-serviceworker v0.1.7 - Guido Zuidhof and contributors, licensed under MIT */
let coepCredentialless = true;
// TabMates note: this file is the vendored coi-serviceworker (COOP/COEP injection so OPFS/SQLite
// works on header-less hosts like GitHub Pages) PLUS an app-shell offline cache added below. The
// caching lives here — not in a second worker — because only one worker can own the root scope and
// that scope must stay with the COOP/COEP injector (see docs/WEB_DEPLOYMENT.md). Every served
// response (network OR cache) is passed through withCoiHeaders() so cross-origin isolation holds
// for offline launches too.
if (typeof window === 'undefined') {
    // Bump this to invalidate the offline app-shell cache on the next activation.
    const SHELL_CACHE = "tabmates-shell-v1";
    // Key the navigation fallback under the app root (start_url is "/").
    const APP_SHELL_URL = "/";

    self.addEventListener("install", () => self.skipWaiting());
    self.addEventListener("activate", (event) =>
        event.waitUntil(
            (async () => {
                // Drop stale shell caches from previous versions.
                const names = await caches.keys();
                await Promise.all(
                    names
                        .filter((n) => n.startsWith("tabmates-shell-") && n !== SHELL_CACHE)
                        .map((n) => caches.delete(n))
                );
                await self.clients.claim();
            })()
        )
    );

    self.addEventListener("message", (ev) => {
        if (!ev.data) {
            return;
        } else if (ev.data.type === "deregister") {
            self.registration
                .unregister()
                .then(() => {
                    return self.clients.matchAll();
                })
                .then(clients => {
                    clients.forEach((client) => client.navigate(client.url));
                });
        } else if (ev.data.type === "coepCredentialless") {
            coepCredentialless = ev.data.value;
        }
    });

    // Re-wrap a response with the COOP/COEP/CORP headers that make the page cross-origin isolated.
    // Opaque responses (status 0) are passed through untouched, exactly as before.
    function withCoiHeaders(response) {
        if (!response || response.status === 0) {
            return response;
        }
        const newHeaders = new Headers(response.headers);
        newHeaders.set("Cross-Origin-Embedder-Policy",
            coepCredentialless ? "credentialless" : "require-corp"
        );
        if (!coepCredentialless) {
            newHeaders.set("Cross-Origin-Resource-Policy", "cross-origin");
        }
        newHeaders.set("Cross-Origin-Opener-Policy", "same-origin");
        return new Response(response.body, {
            status: response.status,
            statusText: response.statusText,
            headers: newHeaders,
        });
    }

    // Only same-origin GETs are cached; Range requests (media seeking) are left to the network so
    // we never cache a partial 206. Everything else keeps the original network-only behaviour.
    function isCacheable(r) {
        if (r.method !== "GET") return false;
        if (r.headers.has("range")) return false;
        try {
            return new URL(r.url).origin === self.location.origin;
        } catch (e) {
            return false;
        }
    }

    self.addEventListener("fetch", function (event) {
        const r = event.request;
        if (r.cache === "only-if-cached" && r.mode !== "same-origin") {
            return;
        }

        const request = (coepCredentialless && r.mode === "no-cors")
            ? new Request(r, {
                credentials: "omit",
            })
            : r;

        // Cross-origin (e.g. gstatic Firebase) and non-GET: original network-only path, unchanged.
        if (!isCacheable(r)) {
            event.respondWith(
                fetch(request)
                    .then((response) => withCoiHeaders(response))
                    .catch((e) => console.error(e))
            );
            return;
        }

        // Navigations: network-first so an online launch is always the latest build; fall back to
        // the cached app shell when offline so an installed PWA still starts.
        if (r.mode === "navigate") {
            event.respondWith(
                (async () => {
                    const cache = await caches.open(SHELL_CACHE);
                    try {
                        const response = await fetch(request);
                        if (response && response.ok) {
                            await cache.put(APP_SHELL_URL, response.clone());
                        }
                        return withCoiHeaders(response);
                    } catch (e) {
                        const cached =
                            (await cache.match(APP_SHELL_URL)) || (await cache.match(request));
                        if (cached) return withCoiHeaders(cached);
                        throw e;
                    }
                })()
            );
            return;
        }

        // Other same-origin assets (composeApp.js, .wasm, skiko, styles, icons, manifest, the
        // SQLite worker): stale-while-revalidate. Serve from cache instantly when present, refresh
        // in the background; content-hashed filenames make new deploys land silently.
        event.respondWith(
            (async () => {
                const cache = await caches.open(SHELL_CACHE);
                const cached = await cache.match(request);
                const network = fetch(request)
                    .then((response) => {
                        if (response && response.ok && response.status !== 0) {
                            cache.put(request, response.clone());
                        }
                        return response;
                    })
                    .catch((e) => {
                        if (!cached) console.error(e);
                        return undefined;
                    });
                if (cached) {
                    event.waitUntil(network);
                    return withCoiHeaders(cached);
                }
                const response = await network;
                return withCoiHeaders(response);
            })()
        );
    });

} else {
    (() => {
        const reloadedBySelf = window.sessionStorage.getItem("coiReloadedBySelf");
        window.sessionStorage.removeItem("coiReloadedBySelf");
        const coepDegrading = (reloadedBySelf == "coepdegrade");

        // You can customize the behavior of this script through a global `coi` variable.
        const coi = {
            shouldRegister: () => !reloadedBySelf,
            shouldDeregister: () => false,
            coepCredentialless: () => true,
            coepDegrade: () => true,
            doReload: () => window.location.reload(),
            quiet: false,
            ...window.coi
        };

        const n = navigator;
        const controlling = n.serviceWorker && n.serviceWorker.controller;

        // Record the failure if the page is served by serviceWorker.
        if (controlling && !window.crossOriginIsolated) {
            window.sessionStorage.setItem("coiCoepHasFailed", "true");
        }
        const coepHasFailed = window.sessionStorage.getItem("coiCoepHasFailed");

        if (controlling) {
            // Reload only on the first failure.
            const reloadToDegrade = coi.coepDegrade() && !(
                coepDegrading || window.crossOriginIsolated
            );
            n.serviceWorker.controller.postMessage({
                type: "coepCredentialless",
                value: (reloadToDegrade || coepHasFailed && coi.coepDegrade())
                    ? false
                    : coi.coepCredentialless(),
            });
            if (reloadToDegrade) {
                !coi.quiet && console.log("Reloading page to degrade COEP.");
                window.sessionStorage.setItem("coiReloadedBySelf", "coepdegrade");
                coi.doReload("coepdegrade");
            }

            if (coi.shouldDeregister()) {
                n.serviceWorker.controller.postMessage({ type: "deregister" });
            }
        }

        // If we're already coi: do nothing. Perhaps it's due to this script doing its job, or COOP/COEP are
        // already set from the origin server. Also if the browser has no notion of crossOriginIsolated, just give up here.
        if (window.crossOriginIsolated !== false || !coi.shouldRegister()) return;

        if (!window.isSecureContext) {
            !coi.quiet && console.log("COOP/COEP Service Worker not registered, a secure context is required.");
            return;
        }

        // In some environments (e.g. Firefox private mode) this won't be available
        if (!n.serviceWorker) {
            !coi.quiet && console.error("COOP/COEP Service Worker not registered, perhaps due to private mode.");
            return;
        }

        n.serviceWorker.register(window.document.currentScript.src).then(
            (registration) => {
                !coi.quiet && console.log("COOP/COEP Service Worker registered", registration.scope);

                registration.addEventListener("updatefound", () => {
                    !coi.quiet && console.log("Reloading page to make use of updated COOP/COEP Service Worker.");
                    window.sessionStorage.setItem("coiReloadedBySelf", "updatefound");
                    coi.doReload();
                });

                // If the registration is active, but it's not controlling the page
                if (registration.active && !n.serviceWorker.controller) {
                    !coi.quiet && console.log("Reloading page to make use of COOP/COEP Service Worker.");
                    window.sessionStorage.setItem("coiReloadedBySelf", "notcontrolling");
                    coi.doReload();
                }
            },
            (err) => {
                !coi.quiet && console.error("COOP/COEP Service Worker failed to register:", err);
            }
        );
    })();
}
