// Service worker for background web push (Firebase Cloud Messaging).
// Registered by firebase-init.js under the ./firebase-cloud-messaging-push-scope scope;
// the root scope belongs to coi-serviceworker.js (COOP/COEP injection).

importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js");

const firebaseApp = firebase.initializeApp({
  apiKey: "AIzaSyD9dQByxdT0_fHCO2FmfrVmvJTi_GyeQDk",
  authDomain: "tabmates-app.firebaseapp.com",
  projectId: "tabmates-app",
  messagingSenderId: "72203045436",
  appId: "1:72203045436:web:8aaceca23465a81c929139"
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
