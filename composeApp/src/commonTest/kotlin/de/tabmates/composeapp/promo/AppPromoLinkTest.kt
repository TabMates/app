package de.tabmates.composeapp.promo

import de.tabmates.composeapp.BuildKonfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPromoLinkTest {
    private val url = androidAppPromoIntentUrl()

    @Test
    fun targetsTheAppLinkHostOnTheClaimedOpenPath() {
        val host = BuildKonfig.BASE_URL_PUBLIC.trimEnd('/').substringAfter("://")

        assertTrue(url.startsWith("intent://$host/open#Intent;"), url)
    }

    @Test
    fun resolvesToTheAndroidPackageOverHttps() {
        assertTrue(url.contains(";scheme=https;"), url)
        assertTrue(url.contains(";package=$ANDROID_PACKAGE;"), url)
        assertTrue(url.endsWith(";end"), url)
    }

    @Test
    fun fallbackUrlIsPercentEncodedSoItCannotTerminateTheExtra() {
        val fallback = url.substringAfter("S.browser_fallback_url=").substringBefore(";end")

        assertFalse(fallback.contains(':'), fallback)
        assertFalse(fallback.contains('/'), fallback)
        assertFalse(fallback.contains('?'), fallback)
        assertFalse(fallback.contains('='), fallback)
        assertFalse(fallback.contains(';'), fallback)
    }

    @Test
    fun fallbackDecodesBackToThePlayListing() {
        val fallback = url.substringAfter("S.browser_fallback_url=").substringBefore(";end")

        assertEquals(PLAY_STORE_URL, percentDecode(fallback))
    }

    private fun percentDecode(value: String): String {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '%') {
                bytes.add(value.substring(index + 1, index + 3).toInt(16).toByte())
                index += 3
            } else {
                bytes.add(char.code.toByte())
                index++
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
