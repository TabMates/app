package de.tabmates.features.notifications.testing

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.notifications.domain.DevicePlatform
import de.tabmates.features.notifications.domain.NotificationService

/** Records calls and returns configurable results. */
open class FakeNotificationService(
    var registerResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var unregisterResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
) : NotificationService {
    val registerCalls: MutableList<Pair<String, DevicePlatform>> = mutableListOf()
    val unregisterCalls: MutableList<String> = mutableListOf()

    override suspend fun registerDevice(
        token: String,
        platform: DevicePlatform,
    ): EmptyResult<DataError.Remote> {
        registerCalls += token to platform
        return registerResult
    }

    override suspend fun unregisterDevice(token: String): EmptyResult<DataError.Remote> {
        unregisterCalls += token
        return unregisterResult
    }
}
