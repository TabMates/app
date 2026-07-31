package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import de.tabmates.features.notifications.domain.PushNotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeNotificationPermissionController(
    initialStatus: NotificationPermissionStatus = NotificationPermissionStatus.GRANTED,
) : NotificationPermissionController {
    private val state = MutableStateFlow(initialStatus)

    override val status: StateFlow<NotificationPermissionStatus> = state

    var refreshCalls: Int = 0
        private set
    var openSettingsCalls: Int = 0
        private set

    override suspend fun refresh() {
        refreshCalls++
    }

    override fun openSettings() {
        openSettingsCalls++
    }

    fun emit(status: NotificationPermissionStatus) {
        state.value = status
    }
}

class FakePushNotificationController : PushNotificationController {
    var startCalls: Int = 0
        private set
    var refreshRegistrationCalls: Int = 0
        private set
    var stopCalls: Int = 0
        private set

    /** Records the order of side effects relative to other collaborators. */
    var onStop: (() -> Unit)? = null

    override fun start() {
        startCalls++
    }

    override suspend fun refreshRegistration() {
        refreshRegistrationCalls++
    }

    override suspend fun stop() {
        stopCalls++
        onStop?.invoke()
    }
}
