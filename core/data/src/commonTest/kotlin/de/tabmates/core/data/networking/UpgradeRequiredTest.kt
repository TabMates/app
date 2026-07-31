package de.tabmates.core.data.networking

import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.update.UpgradeRequiredNotifier
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpgradeRequiredTest {
    private class NoOpLogger : TabMatesLogger {
        override fun debug(
            tag: String,
            message: String,
        ) = Unit

        override fun info(
            tag: String,
            message: String,
        ) = Unit

        override fun warning(
            tag: String,
            message: String,
        ) = Unit

        override fun error(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit
    }

    private class EmptySessionStorage : SessionStorage {
        override val authState: StateFlow<AuthInfo?> = MutableStateFlow(null)

        override fun get(): AuthInfo? = null

        override fun set(info: AuthInfo?) = Unit
    }

    private class NoOpSessionInvalidator : SessionInvalidator {
        override fun invalidate(reason: SessionInvalidationReason) = Unit
    }

    private fun clientRespondingWith(
        status: HttpStatusCode,
        notifier: UpgradeRequiredNotifier,
    ) = HttpClientFactory(
        tabMatesLogger = NoOpLogger(),
        sessionStorage = EmptySessionStorage(),
        json = Json { ignoreUnknownKeys = true },
        upgradeRequiredNotifier = notifier,
        sessionInvalidator = NoOpSessionInvalidator(),
    ).create(MockEngine { respondError(status) })

    @Test
    fun `426 maps to UPGRADE_REQUIRED rather than the generic unknown error`() =
        runTest {
            val client = clientRespondingWith(HttpStatusCode.UpgradeRequired, UpgradeRequiredNotifier())

            val result = client.get<String>("/api/anything")

            assertTrue(result is Result.Failure)
            assertEquals(DataError.Remote.UPGRADE_REQUIRED, result.error)
        }

    @Test
    fun `426 on any endpoint flips the upgrade-required notifier`() =
        runTest {
            val notifier = UpgradeRequiredNotifier()
            val client = clientRespondingWith(HttpStatusCode.UpgradeRequired, notifier)
            assertFalse(notifier.isUpgradeRequired.value)

            client.get<String>("/api/anything")

            assertTrue(notifier.isUpgradeRequired.value)
        }

    @Test
    fun `other failures leave the notifier alone`() =
        runTest {
            val notifier = UpgradeRequiredNotifier()
            val client = clientRespondingWith(HttpStatusCode.InternalServerError, notifier)

            client.get<String>("/api/anything")

            assertFalse(notifier.isUpgradeRequired.value)
        }

    @Test
    fun `the client version header is platform slash version`() {
        assertEquals("${AppBuildInfo.platform}/${AppBuildInfo.version}", AppBuildInfo.clientVersionHeader)
    }
}
