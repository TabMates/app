package de.tabmates.features.notifications.data

import de.tabmates.core.data.networking.delete
import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.preferences.LocaleProvider
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.features.notifications.data.dto.requests.RegisterDeviceRequest
import de.tabmates.features.notifications.domain.DevicePlatform
import de.tabmates.features.notifications.domain.NotificationService
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single(binds = [NotificationService::class])
class KtorNotificationService(
    private val httpClient: HttpClient,
    private val localeProvider: LocaleProvider,
) : NotificationService {
    override suspend fun registerDevice(
        token: String,
        platform: DevicePlatform,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/notification/register",
            body =
                RegisterDeviceRequest(
                    token = token,
                    platform = platform.wireValue,
                    locale = localeProvider.currentLanguageTag(),
                ),
        )
    }

    override suspend fun unregisterDevice(token: String): EmptyResult<DataError.Remote> {
        return httpClient.delete(
            route = "/api/notification/$token",
        )
    }
}
