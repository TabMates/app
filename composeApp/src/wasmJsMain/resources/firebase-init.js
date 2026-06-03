// Firebase Cloud Messaging (web) glue, driven from Kotlin/Wasm (WebPushNotificationController).
// Loaded by index.html after the Firebase compat SDK scripts.
// TODO: replace the placeholders with your Firebase web app config + Web Push VAPID key.

const firebaseConfig = {
  apiKey: "REPLACE_ME",
  authDomain: "REPLACE_ME.firebaseapp.com",
  projectId: "REPLACE_ME",
  messagingSenderId: "REPLACE_ME",
  appId: "REPLACE_ME",
};

// Cloud Messaging -> Web configuration -> "Web Push certificates" key pair.
const VAPID_KEY = "REPLACE_ME";

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
window.tabmatesFcmRequestToken = function () {
  return navigator.serviceWorker
    .register("firebase-messaging-sw.js")
    .then(function (registration) {
      return Notification.requestPermission().then(function (permission) {
        if (permission !== "granted") return null;
        return messaging.getToken({ vapidKey: VAPID_KEY, serviceWorkerRegistration: registration });
      });
    })
    .catch(function (error) {
      console.error("FCM token error", error);
      return null;
    });
};
