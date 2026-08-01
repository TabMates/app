package de.tabmates.core.data.environment

import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.util.Result
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvironmentUrlsTest {
    @Test
    fun httpsUrl_isAccepted() {
        assertEquals("https://api.example.com", normalize("https://api.example.com"))
    }

    @Test
    fun trailingSlashAndWhitespace_areTrimmed() {
        assertEquals("https://api.example.com", normalize("  https://api.example.com/  "))
    }

    @Test
    fun portAndPath_areKept() {
        assertEquals("https://api.example.com:8443/gateway", normalize("https://api.example.com:8443/gateway"))
    }

    @Test
    fun queryAndFragment_areDropped() {
        // A pasted link must not smuggle parameters into every request the app makes.
        assertEquals("https://api.example.com", normalize("https://api.example.com/?token=abc#top"))
    }

    @Test
    fun httpUrl_isRejectedAsInsecure() {
        assertEquals(EnvironmentSwitchError.INSECURE_URL, rejection("http://api.example.com"))
    }

    @Test
    fun urlWithoutScheme_isRejected() {
        assertEquals(EnvironmentSwitchError.INVALID_URL, rejection("api.example.com"))
    }

    @Test
    fun blankUrl_isRejected() {
        assertEquals(EnvironmentSwitchError.INVALID_URL, rejection("   "))
    }

    @Test
    fun schemeWithoutHost_isRejected() {
        assertEquals(EnvironmentSwitchError.INVALID_URL, rejection("https://"))
    }

    @Test
    fun webSocketUrl_swapsSchemeAndAppendsWsPath() {
        assertEquals("wss://api.example.com/ws", EnvironmentUrls.toWebSocketBaseUrl("https://api.example.com"))
    }

    @Test
    fun webSocketUrl_keepsExplicitPort() {
        assertEquals(
            "wss://api.example.com:8443/ws",
            EnvironmentUrls.toWebSocketBaseUrl("https://api.example.com:8443"),
        )
    }

    @Test
    fun webSocketUrl_downgradesToWsForPlainHttp() {
        // Not reachable through normalizeHttpBaseUrl, but the build-time BASE_URL_HTTP is a local
        // dev backend often enough for this to be the common path there.
        assertEquals("ws://10.0.2.2:8080/ws", EnvironmentUrls.toWebSocketBaseUrl("http://10.0.2.2:8080"))
    }

    @Test
    fun webSocketUrl_trimsTrailingSlash() {
        assertEquals("wss://api.example.com/ws", EnvironmentUrls.toWebSocketBaseUrl("https://api.example.com/"))
    }

    @Test
    fun webSocketUrl_keepsPathPrefix() {
        assertEquals(
            "wss://api.example.com/gateway/ws",
            EnvironmentUrls.toWebSocketBaseUrl("https://api.example.com/gateway"),
        )
    }

    private fun normalize(raw: String): String =
        when (val result = EnvironmentUrls.normalizeHttpBaseUrl(raw)) {
            is Result.Success -> result.data
            is Result.Failure -> error("Expected success but got ${result.error}")
        }

    private fun rejection(raw: String): EnvironmentSwitchError =
        when (val result = EnvironmentUrls.normalizeHttpBaseUrl(raw)) {
            is Result.Success -> error("Expected failure but got ${result.data}")
            is Result.Failure -> result.error
        }
}
