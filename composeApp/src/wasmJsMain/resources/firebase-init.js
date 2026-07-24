// Firebase Cloud Messaging (web) glue, driven from Kotlin/Wasm (WebPushNotificationController).
// Loaded by index.html after the Firebase compat SDK scripts.
// The Web Push VAPID key is not hardcoded here — it's injected at build time via BuildKonfig
// (FCM_VAPID_KEY) and passed into tabmatesFcmRequestToken() below.

const firebaseConfig = {
  apiKey: "AIzaSyD9dQByxdT0_fHCO2FmfrVmvJTi_GyeQDk",
  authDomain: "tabmates-app.firebaseapp.com",
  projectId: "tabmates-app",
  messagingSenderId: "72203045436",
  appId: "1:72203045436:web:8aaceca23465a81c929139"
};

// The Firebase compat SDK is loaded cross-origin from gstatic.com, which is unreachable when the
// installed PWA launches offline. This file is same-origin (cached + served by the app-shell
// service worker), so it still runs — guard against the missing SDK and expose no-op glue so the
// Wasm app boots normally offline. Push simply stays unavailable until the next online launch.
if (typeof firebase === "undefined" || typeof firebase.messaging !== "function") {
  window.tabmatesFcmInit = function () {};
  window.tabmatesFcmRequestToken = function () { return Promise.resolve(null); };
} else {

const firebaseApp = firebase.initializeApp(firebaseConfig);
// Disable Firebase telemetry; only Cloud Messaging is used (no getAnalytics()).
firebaseApp.automaticDataCollectionEnabled = false;
const messaging = firebase.messaging();

// Set up foreground message handling. Called once on app start.
window.tabmatesFcmInit = function () {
  messaging.onMessage(function (payload) {
    if (Notification.permission !== "granted") return;
    const n = payload.notification || {};
    const data = payload.data || {};
    const notification = new Notification(n.title || "TabMates", { body: n.body || "" });
    notification.onclick = function () {
      if (data.deepLink) window.location.href = data.deepLink;
      window.focus();
    };
  });
};

// Requests notification permission and returns the FCM web token (or null). Returns a Promise.
// vapidKey comes from Kotlin (BuildKonfig.FCM_VAPID_KEY) — see WebPushNotificationController.
window.tabmatesFcmRequestToken = function (vapidKey) {
  // Explicit non-root scope: the root scope belongs to coi-serviceworker.js (COOP/COEP
  // injection); registering here without a scope would silently replace it.
  return navigator.serviceWorker
    .register("firebase-messaging-sw.js", { scope: "./firebase-cloud-messaging-push-scope" })
    .then(function (registration) {
      return Notification.requestPermission().then(function (permission) {
        if (permission !== "granted") return null;
        return messaging.getToken({ vapidKey: vapidKey, serviceWorkerRegistration: registration });
      });
    })
    .catch(function (error) {
      console.error("FCM token error", error);
      return null;
    });
};

}
