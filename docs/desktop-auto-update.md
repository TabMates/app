# Desktop auto-update (Conveyor)

Desktop (macOS, Windows, Linux) ships via [Conveyor](https://conveyor.hydraulic.dev/).
Conveyor builds signed, self-updating packages: each app polls the update site on launch and
updates itself silently in the background, applying the update on next restart.

Because of this, the app's own update check (`AppUpdateRepository` / `AppUpdateHandler`) is a
**no-op on desktop** — Conveyor owns the whole flow. The check still runs on Android (Play in-app
update) and iOS (store-redirect dialog); web is skipped.

## Module layout

- `desktopApp/` — thin JVM launcher + packaging module. Depends on `composeApp`, applies the
  Conveyor Gradle plugin, and declares the per-OS Compose/Skiko bundles. Kept separate because
  Conveyor's cross-OS dependency configurations leak into `composeApp`'s iOS/wasm variant
  resolution if added there.
- `conveyor.conf` (repo root) — app metadata, update-site URL, signing config. Includes the Gradle
  export (`:desktopApp:printConveyorConfig`) for main class, classpath, JDK and version.
- App version flows from `APP_VERSION` (gradle.properties) → `project.version` → `app.version`.

## What still needs to be provided (cannot ship without these)

1. **Update-site host** — set `app.site.base-url` in `conveyor.conf` to a real, reachable URL
   (S3/CDN bucket, or `github:TabMates/app` for GitHub Releases). No host = no auto-update.
2. **Code signing**
   - macOS: Apple Developer ID certificate + notarization (else Gatekeeper blocks launch/update).
   - Windows: Authenticode certificate (else SmartScreen warns).
   - Linux: unsigned is fine.
3. **App icons** — uncomment `app.icons` in `conveyor.conf` and add a square PNG/SVG.

## Release flow

1. Bump `APP_VERSION` in `gradle.properties`.
2. Install the Conveyor CLI (`brew install hydraulicco/tap/conveyor`, or see the docs).
3. `conveyor make site` — builds packages for all three OSes and the update metadata, signing
   along the way. Conveyor invokes Gradle itself via the `include` in `conveyor.conf`.
4. Upload the generated `output/` to the update-site host.

Returning users auto-update from the previous release; new users download from the site's
download page.

## Forced updates on desktop

Conveyor applies updates on the next restart and does not hard-block an outdated session. There is
no min-supported-version gate on desktop (unlike Android/iOS via the backend check). If a hard gate
is ever needed, add a server check + blocking dialog in `AppUpdateHandler.desktop.kt`.
