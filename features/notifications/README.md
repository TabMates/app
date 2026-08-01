# :features:notifications

Notifications across all targets, behind one `PushNotificationController` per platform.
Delivery differs per platform because Firebase Cloud Messaging (FCM) push only works on
Android + iOS:

| Platform | Mechanism |
|----------|-----------|
| Android  | FCM push via [kmpnotifier](https://github.com/mirzemehdi/KMPNotifier) |
| iOS      | FCM push via kmpnotifier (Swift AppDelegate bridges APNs into Firebase) |
| Desktop  | No FCM client exists → backend WebSocket stream rendered as local notifications (kmpnotifier local notifier) |
| Web      | Firebase **JS SDK** push (service worker + VAPID), not kmpnotifier |

## Layout

- **domain** — `NotificationService` (backend token registration), `PushNotificationController`
  (platform entry point), `DevicePlatform`, `PushNotificationConstants`, `NotificationDeepLinkBus`
  (data publishes clicked-notification deep links, the app collects and navigates).
- **data** — `KtorNotificationService` (backend calls) and the per-platform
  `PlatformNotificationsModule` Koin modules, each providing one controller:
  - `MobilePushNotificationController` — `androidMain` + `iosMain` (FCM via kmpnotifier).
  - `DesktopPushNotificationController` — `desktopMain` (Ktor WebSocket → kmpnotifier local notifier).
  - `WebPushNotificationController` — `webMain` (Firebase JS SDK via `js()` glue).
  - `NoOpPushNotificationController` — fallback, unused now.

`NotificationsSyncCoordinator` (in `:composeApp`) starts the controller on login, unregisters
on logout, and re-registers on in-app language change — mirrors `GroupSyncCoordinator`.

> Note: the mobile controller lives in `androidMain` and `iosMain` separately (not a shared
> `mobileMain`) because this project's AGP KMP android target is not part of the
> `mobile` hierarchy group, so `mobileMain` does not reach the android compilation.

## Backend contract

The bearer token on the shared `HttpClient` identifies the user; endpoints associate a
device token with that user. Server side: TabMatesServer `notification` module
(`DeviceTokenController`).

- `POST /api/notification/register` — body `{ "token": String, "platform": "ANDROID"|"IOS"|"DESKTOP"|"WEB", "locale": String }` (idempotent upsert; re-registering refreshes owner, platform, and locale)
- `DELETE /api/notification/{token}` — no body
- `GET /api/notifications/stream` (WebSocket) — Desktop only. Server-push-only stream of
  `NotificationEventDto { title, body, deepLink? }` frames for platforms without FCM. Auth rides
  the shared `HttpClient` (`Authorization: Bearer` + `x-api-key`); locale is supplied as a `lang`
  BCP-47 query param (stream clients register no device token, so `lang` is the only locale
  signal — absent/unsupported → English).

`locale` is a BCP-47 tag (e.g. `"de"`, `"en-US"`). The backend should localize the push
`notification` block (title/body) in that language — localizing client-side is unreliable
because the OS renders pushes while the app is killed. The device re-registers automatically
when the in-app language changes (`NotificationsSyncCoordinator`); the desktop stream likewise
reconnects with the new `lang` (`DesktopPushNotificationController.refreshRegistration()`).

Push payloads include `deepLink` (`PushNotificationConstants.KEY_DEEP_LINK`) — the URL the
notification opens on tap. For a group event the backend sends `https://<host>/groups/<groupId>`,
which resolves to the group detail screen (`App.kt` registers the deep-link route). The backend
pushes on: entry added/updated/deleted (expense/income/settlement) and member joined, always
excluding the acting user's own devices, with `android.notification.channel_id` set to
`expenses`/`settle_ups`/`members` accordingly.

### In-app language

Language resolves via `LocaleProvider`: the pinned `AppLanguage` (stored in
`AppPreferencesRepository`) if set, else the device locale (`deviceLanguageTag()`). To add a
language settings screen later, surface `AppLanguage` entries and call `setAppLanguage(...)`;
notification re-registration and the `locale` field follow automatically. Actually applying
the chosen language to the UI (Compose resources / per-app locales) is a separate step.

## Config & secrets in a public repo

Firebase **client** config (`google-services.json`, `GoogleService-Info.plist`, the web
`apiKey`/`appId`) is **not secret** — it ships in every binary/page. Security comes from
**Firebase Security Rules + App Check + API-key restrictions**, not from hiding these files.
The only true secrets (never commit): the **FCM server key / service-account JSON** (backend
only) and the **VAPID private key** (stays in Firebase).

Handling per platform:
- **Android** — `androidApp/google-services.json` is **committed with dummy values** so the repo
  builds out of the box (the `google-services` Gradle plugin fails the build if the file is
  missing). Replace it with the real file from Firebase Console for working push. It's not
  secret, so committing dummy IDs is fine.
- **iOS** — `GoogleService-Info.plist` is git-ignored (it's a runtime resource for the Xcode app
  and doesn't block Gradle builds); each contributor adds their own to the Xcode target.
- **Web** — `firebaseConfig` (apiKey/authDomain/projectId/etc.) is committed directly in
  `firebase-init.js`/`firebase-messaging-sw.js` — it's served to the browser either way, so
  there's nothing to gain by templating it; restrict the API key by HTTP referrer + App Check
  instead. `FCM_VAPID_KEY` is injected via BuildKonfig (env var / `local.properties`) rather than
  hardcoded, mainly so CI/local/staging can point at different Firebase projects without editing
  source.

## Telemetry

Firebase Analytics / data collection is **disabled** — only Cloud Messaging is used:
- Android: `firebase_analytics_collection_deactivated` + ad-id/ssaid flags in the app manifest.
- iOS: `FIREBASE_ANALYTICS_COLLECTION_DEACTIVATED` in `Info.plist`; do **not** link the
  `FirebaseAnalytics` SPM product (FirebaseMessaging alone collects no analytics).
- Web: only `firebase.messaging()` is initialized (never `getAnalytics()`), and
  `automaticDataCollectionEnabled = false`.

FCM message delivery metrics (BigQuery export) are off by default in the Firebase Console.

## Manual setup still required

These need a real Firebase project and the native toolchains and cannot be committed here.

### Android
1. Create a Firebase Android app (package `de.tabmates.androidapp`).
2. Replace the committed **dummy** `androidApp/google-services.json` with the real one from
   Firebase Console. (The dummy lets the repo build; push only works with the real file.)
3. Replace the placeholder notification icon in
   `PlatformNotificationsModule.android.kt` (`android.R.drawable.ic_dialog_info`) with a
   branded monochrome icon.

### Android notification categories (channels)
`NotificationChannels` (in `:androidApp`) creates four channels on launch: `general` (default),
`expenses`, `members`, `settle_ups`. To route a **background** push to a category, the backend
sets `android.notification.channel_id` to one of those ids; with no id the manifest default
(`general`) is used.

Limitation: kmpnotifier owns **foreground** display and uses its own channel (it exposes no
channel config), so categories apply to system-displayed background notifications, not foreground
ones. iOS/Web have no equivalent channel concept here.

### iOS
1. Create a Firebase iOS app; add `GoogleService-Info.plist` to the Xcode `iosApp` target.
2. Link the Firebase iOS SDK (FirebaseCore + FirebaseMessaging) via **Swift Package Manager**
   in `iosApp.xcodeproj` (File → Add Package Dependencies… → `https://github.com/firebase/firebase-ios-sdk`,
   select the `FirebaseCore` + `FirebaseMessaging` products for the `iosApp` target). KMPNotifier 2.0
   consumes Firebase via SPM (`swiftPMDependencies`), not the CocoaPods Gradle plugin. The
   `iOSApp.swift` AppDelegate already calls `FirebaseApp.configure()` and forwards APNs /
   remote-notification callbacks into kmpnotifier via `KMPNotifier.onApplicationDidReceiveRemoteNotification`.
3. In the target's Signing & Capabilities, add **Push Notifications** and **Background Modes →
   Remote notifications**.
4. Upload the APNs auth key to Firebase Console.

### Desktop (WebSocket → local notifications)
No FCM on desktop. The app opens a WebSocket to `<ws base>/api/notifications/stream` (the ws base
is derived from `BASE_URL_HTTP`; see `EnvironmentUrls`)
(`DesktopPushNotificationController`) and renders each `NotificationEventDto`
(`{ title, body, deepLink? }`) as a local notification.
1. **Backend**: implement the authenticated WS endpoint pushing `NotificationEventDto` JSON
   frames to the logged-in user.
2. Set a real icon path in `DesktopPushNotificationController` (`notificationIconPath`,
   currently `""`).

### Web (Firebase JS SDK)
Real browser push via the Firebase **JS** SDK — independent of kmpnotifier.
1. Create a Firebase Web app; copy its config into **both**
   `composeApp/src/wasmJsMain/resources/firebase-init.js` and `firebase-messaging-sw.js`
   (replace the placeholders).
2. Generate a **Web Push certificate (VAPID key)** in Firebase Console → Cloud Messaging →
   Web configuration, and set it as the `FCM_VAPID_KEY` build property (`local.properties` or
   CI env var — see root README). It's injected via BuildKonfig and passed into
   `tabmatesFcmRequestToken()` at call time, not hardcoded in `firebase-init.js`.
3. `firebase-messaging-sw.js` must be served from the web root (it already lives in
   `wasmJsMain/resources`). `index.html` loads the Firebase compat SDK + `firebase-init.js`.
4. Web push requires HTTPS (or `localhost`).
