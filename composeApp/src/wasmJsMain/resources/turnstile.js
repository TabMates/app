// Cloudflare Turnstile (invisible/managed) glue, driven from Kotlin/Wasm (WebTurnstileTokenProvider).
// Loaded by index.html after the Turnstile api.js (?render=explicit, so we control render()).
//
// The api.js is cross-origin from challenges.cloudflare.com and unreachable when the installed PWA
// launches offline. This file is same-origin (cached + served by the app-shell service worker), so
// it still runs — every entry point guards against a missing/blocked api.js and falls back to a null
// token, so the Wasm app boots normally and auth calls simply omit the cf-turnstile-response header.
//
// The widget container is created here rather than declared in index.html: ComposeViewport mounts
// into document.body and clears its children, so any node written in the HTML body is gone by the
// time the app runs (see composeApp/src/webMain/kotlin/de/tabmates/composeapp/main.kt).
(function () {
  var CONTAINER_ID = "turnstile-container";
  // Hard cap on a single challenge. Turnstile's timeout-callback only covers challenges it knows
  // expired — a challenge that simply never returns would otherwise leave getToken() suspended
  // forever and the submit button spinning.
  var EXECUTE_TIMEOUT_MS = 15000;

  var widgetId = null;
  var container = null;
  // The container widgetId was rendered into, so a Compose-dropped container forces a re-render.
  var renderedIn = null;
  var sitekey = null;
  // Resolver for the challenge currently in flight; Turnstile delivers the token via callback.
  var pendingResolve = null;
  var pendingTimer = null;

  function warn(reason) {
    console.warn("[turnstile] no token: " + reason);
  }

  function settle(value) {
    if (pendingTimer !== null) {
      clearTimeout(pendingTimer);
      pendingTimer = null;
    }
    if (pendingResolve) {
      var resolve = pendingResolve;
      pendingResolve = null;
      resolve(value);
    }
  }

  // Returns the widget container, (re-)creating it if it was never made or Compose removed it.
  // Not display:none — an interaction-only widget collapses to nothing on its own while the
  // challenge is silent, but must be able to show itself on the rare challenge that needs a click.
  // Fixed + centered so that one lands on top of the Compose canvas instead of inside its layout.
  function getContainer() {
    if (container && document.body.contains(container)) return container;
    if (!document.body) return null;
    container = document.createElement("div");
    container.id = CONTAINER_ID;
    container.style.position = "fixed";
    container.style.left = "50%";
    container.style.top = "50%";
    container.style.transform = "translate(-50%, -50%)";
    container.style.zIndex = "2147483647";
    document.body.appendChild(container);
    return container;
  }

  // Renders the widget once. Returns false if the api.js/DOM are not ready yet.
  function ensureRendered() {
    var target = getContainer();
    if (!target) {
      warn("document.body not ready");
      return false;
    }
    // A container we already rendered into can be dropped by Compose; re-render into the new one.
    if (renderedIn !== target) widgetId = null;
    if (widgetId !== null) return true;
    if (typeof window.turnstile === "undefined") {
      warn("api.js unavailable (offline or blocked)");
      return false;
    }
    if (!sitekey) {
      warn("no site key (TURNSTILE_SITE_KEY unset for this build)");
      return false;
    }
    try {
      widgetId = window.turnstile.render(target, {
        sitekey: sitekey,
        // "invisible" is NOT a valid size (only compact/flexible/normal) — passing it makes
        // render() throw. Invisible-until-needed is appearance:interaction-only instead.
        appearance: "interaction-only",
        // Challenge runs only when execute() is called (on submit), not at render time.
        execution: "execute",
        callback: function (token) { settle(token); },
        "error-callback": function () { settle(null); return true; },
        "timeout-callback": function () { settle(null); },
      });
    } catch (e) {
      widgetId = null;
      warn("render() threw: " + e);
      return false;
    }
    // render() returns undefined when it declines to render; undefined !== null, so normalise it
    // or the widget would count as rendered forever.
    if (widgetId === undefined || widgetId === null) {
      widgetId = null;
      warn("render() returned no widget id");
      return false;
    }
    renderedIn = target;
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
      pendingTimer = setTimeout(function () {
        warn("challenge timed out after " + EXECUTE_TIMEOUT_MS + "ms");
        settle(null);
      }, EXECUTE_TIMEOUT_MS);
      try {
        window.turnstile.reset(widgetId);
        window.turnstile.execute(widgetId);
      } catch (e) {
        warn("execute() threw: " + e);
        settle(null);
      }
    });
  };
})();
