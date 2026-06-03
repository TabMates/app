// Service worker for background web push (Firebase Cloud Messaging).
// Must be served from the web root as /firebase-messaging-sw.js.
// TODO: keep firebaseConfig in sync with firebase-init.js.

importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js");

const firebaseApp = firebase.initializeApp({
  apiKey: "REPLACE_ME",
  authDomain: "REPLACE_ME.firebaseapp.com",
  projectId: "REPLACE_ME",
  messagingSenderId: "REPLACE_ME",
  appId: "REPLACE_ME",
});
// Disable Firebase telemetry; only Cloud Messaging is used.
firebaseApp.automaticDataCollectionEnabled = false;

const messaging = firebase.messaging();

messaging.onBackgroundMessage(function (payload) {
  const n = payload.notification || {};
  const data = payload.data || {};
  self.registration.showNotification(n.title || "TabMates", {
    body: n.body || "",
    data: { deepLink: data.deepLink || "/" },
  });
});

self.addEventListener("notificationclick", function (event) {
  event.notification.close();
  const deepLink = (event.notification.data && event.notification.data.deepLink) || "/";
  event.waitUntil(clients.openWindow(deepLink));
});
