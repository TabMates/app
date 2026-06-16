package de.tabmates.composeapp.update

import androidx.compose.runtime.Composable
import de.tabmates.features.appupdate.domain.AppUpdateStatus

// The web app is always served fresh, so the update check never runs (see AppUpdateRepository).
// This actual exists only to satisfy the expect declaration.
@Composable
actual fun AppUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
) = Unit
