package de.tabmates.composeapp.update

import androidx.compose.runtime.Composable
import de.tabmates.features.appupdate.domain.AppUpdateStatus

@Composable
actual fun AppUpdateHandler(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
) = DefaultUpdateHandler(status = status, onDismiss = onDismiss)
