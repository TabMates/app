package de.tabmates.composeapp.update

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import de.tabmates.features.appupdate.domain.AppUpdateStatus

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val PLAY_SERVICES_PACKAGE = "com.google.android.gms"

/**
 * Android update handler. When the app is installed from the Play Store on a device with Play
 * Services, it runs the native in-app update flow (immediate for [AppUpdateStatus.Forced], flexible
 * for [AppUpdateStatus.Optional]). Otherwise — sideloaded, or no Play Services — it falls back to the
 * shared store-redirect dialog ([DefaultUpdateHandler]), same as iOS/desktop.
 */
@Composable
actual fun AppUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
) {
    if (status is AppUpdateStatus.UpToDate) return

    val context = LocalContext.current
    val activity = LocalActivity.current

    val playEligible =
        remember { activity != null && context.isInstalledFromPlayStore() && context.hasPlayServices() }

    if (!playEligible || activity == null) {
        DefaultUpdateHandler(status = status, onDismiss = onDismiss)
        return
    }

    val forced = status is AppUpdateStatus.Forced
    val manager = remember { AppUpdateManagerFactory.create(context) }
    // Set when the native flow can't run (no update offered, user cancelled a forced update, or an
    // error). Triggers the store-redirect fallback so the user is never left stuck.
    var nativeFailed by remember { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // A cancelled immediate (forced) update must not leave the app usable — push the fallback.
            if (forced && result.resultCode != Activity.RESULT_OK) nativeFailed = true
        }

    // Flexible updates download in the background; install once ready.
    DisposableEffect(manager, forced) {
        val listener =
            InstallStateUpdatedListener { state ->
                if (!forced && state.installStatus() == InstallStatus.DOWNLOADED) {
                    manager.completeUpdate()
                }
            }
        manager.registerListener(listener)
        onDispose { manager.unregisterListener(listener) }
    }

    LaunchedEffect(status) {
        val type = if (forced) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
        val info: AppUpdateInfo? = runCatching { manager.requestAppUpdateInfo() }.getOrNull()
        val availability = info?.updateAvailability()
        val canUpdate =
            info != null &&
                info.isUpdateTypeAllowed(type) &&
                (
                    availability == UpdateAvailability.UPDATE_AVAILABLE ||
                        // Resume an immediate update that was interrupted mid-flow.
                        (forced && availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                )

        if (canUpdate) {
            runCatching {
                manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
            }.onFailure { nativeFailed = true }
        } else {
            nativeFailed = true
        }
    }

    if (nativeFailed) {
        DefaultUpdateHandler(status = status, onDismiss = onDismiss)
    }
}

/** True if the Play Store is the recorded installer of this app. */
private fun Context.isInstalledFromPlayStore(): Boolean {
    val installer =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        }.getOrNull()
    return installer == PLAY_STORE_PACKAGE
}

/** True if Google Play Services is present (in-app updates require it). */
private fun Context.hasPlayServices(): Boolean =
    runCatching { packageManager.getPackageInfo(PLAY_SERVICES_PACKAGE, 0) }.isSuccess
