package de.tabmates.composeapp.update

import androidx.compose.runtime.Composable
import de.tabmates.features.appupdate.domain.AppUpdateStatus

/**
 * Where the FOSS build sends a user who needs to update.
 *
 * The backend answers `/api/app-version?platform=android` with the Play listing, which is the
 * wrong destination for someone who installed from F-Droid, so this overrides it. The releases
 * page is correct from the first FOSS build onwards; point it at the F-Droid listing
 * (`https://f-droid.org/packages/de.tabmates.androidapp/`) once the app is actually published
 * there.
 */
private const val FOSS_STORE_URL = "https://github.com/TabMates/app/releases/latest"

/**
 * FOSS (F-Droid) update handler.
 *
 * Google Play Core is proprietary and absent from this build, so there is no native in-app update
 * flow — the shared store-redirect dialog handles it, exactly as on iOS and desktop. F-Droid
 * clients update apps themselves; this dialog exists for the forced-update gate, where the user
 * has to be told the build is no longer supported.
 */
@Composable
actual fun AppUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
) {
    DefaultUpdateHandler(status = status, onDismiss = onDismiss, updateUrlOverride = FOSS_STORE_URL)
}
