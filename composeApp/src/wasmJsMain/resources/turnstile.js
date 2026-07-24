// Cloudflare Turnstile (invisible/managed) glue, driven from Kotlin/Wasm (WebTurnstileTokenProvider).
// Loaded by index.html after the Turnstile api.js (?render=explicit, so we control render()).
//
// The api.js is cross-origin from challenges.cloudflare.com and unreachable when the installed PWA
// launches offline. This file is same-origin (cached + served by the app-shell service worker), so
// it still runs — every entry point guards against a missing/blocked api.js and falls back to a null
// token, so the Wasm app boots normally and auth calls simply omit the cf-turnstile-response header.
(function () {
  var widgetId = null;
  var sitekey = null;
  // Resolver for the challenge currently in flight; Turnstile delivers the token via callback.
  var pendingResolve = null;

  function settle(value) {
    if (pendingResolve) {
      var resolve = pendingResolve;
      pendingResolve = null;
      resolve(value);
    }
  }

  // Renders the invisible widget once. Returns false if the api.js/container are not ready yet.
  function ensureRendered() {
    if (widgetId !== null) return true;
    if (typeof window.turnstile === "undefined" || !sitekey) return false;
    var container = document.getElementById("turnstile-container");
    if (!container) return false;
    widgetId = window.turnstile.render(container, {
      sitekey: sitekey,
      size: "invisible",
      // Challenge runs only when execute() is called (on submit), not at render time.
      execution: "execute",
      callback: function (token) { settle(token); },
      "error-callback": function () { settle(null); return true; },
      "timeout-callback": function () { settle(null); },
    });
    return true;
  }

  // Called once at startup with the site key. Renders when the Turnstile API is ready.
  window.tabmatesTurnstileInit = function (key) {
    sitekey = key;
    if (window.turnstile && typeof window.turnstile.ready === "function") {
      window.turnstile.ready(ensureRendered);
    } else {
      ensureRendered();
    }
  };

  // Runs a fresh challenge and resolves with the token (or null on failure/unavailable). Resets
  // first so each submit uses a single-use token.
  window.tabmatesTurnstileExecute = function () {
    return new Promise(function (resolve) {
      if (!ensureRendered()) { resolve(null); return; }
      // Abandon any prior in-flight challenge before starting a new one.
      settle(null);
      pendingResolve = resolve;
      try {
        window.turnstile.reset(widgetId);
        window.turnstile.execute(widgetId);
      } catch (e) {
        settle(null);
      }
    });
  };
})();
