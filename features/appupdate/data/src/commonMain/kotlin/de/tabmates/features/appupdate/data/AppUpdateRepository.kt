package de.tabmates.features.appupdate.data

import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.data.WEB_PLATFORM
import de.tabmates.core.data.clientPlatform
import de.tabmates.core.data.networking.get
import de.tabmates.core.domain.util.Result
import de.tabmates.features.appupdate.domain.AppUpdateStatus
import de.tabmates.features.appupdate.domain.isVersionLower
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

/**
 * Checks whether the installed app is outdated by comparing [AppBuildInfo.version] against the
 * remote version info. Fails open: any network/parse error yields [AppUpdateStatus.UpToDate] so a
 * flaky endpoint never blocks the user.
 */
@Single
class AppUpdateRepository(
    private val httpClient: HttpClient,
) {
    suspend fun check(): AppUpdateStatus {
        if (clientPlatform == WEB_PLATFORM) return AppUpdateStatus.UpToDate

        val dto =
            when (val result = httpClient.get<AppVersionDto>(ROUTE, mapOf("platform" to clientPlatform))) {
                is Result.Success -> result.data
                is Result.Failure -> return AppUpdateStatus.UpToDate
            }
        return dto.toStatus(current = AppBuildInfo.version)
    }

    private fun AppVersionDto.toStatus(current: String): AppUpdateStatus =
        when {
            isVersionLower(current, minSupportedVersion) -> AppUpdateStatus.Forced(updateUrl, latestVersion)
            isVersionLower(current, latestVersion) -> AppUpdateStatus.Optional(updateUrl, latestVersion)
            else -> AppUpdateStatus.UpToDate
        }

    private companion object {
        private const val ROUTE = "/api/app-version"
    }
}
