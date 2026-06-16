package de.tabmates.composeapp.update

import androidx.compose.runtime.Composable
import de.tabmates.features.appupdate.domain.AppUpdateStatus

// Desktop updates are handled natively by Conveyor (silent background download on macOS/Windows/
// Linux, applied on next restart) — see conveyor.conf. The status is always UpToDate here because
// AppUpdateRepository skips the server check for desktop, so this actual is a no-op.
@Composable
actual fun AppUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
) = Unit
