package de.tabmates.composeapp.deeplink

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkHandlerTest {

    @AfterTest
    fun tearDown() {
        DeepLinkHandler.listener = null
    }

    @Test
    fun onDeepLinkInvokesListenerImmediately() {
        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        DeepLinkHandler.onDeepLink("https://example.com/test")

        assertEquals("https://example.com/test", received)
    }

    @Test
    fun onDeepLinkCachesUriWhenNoListenerIsSet() {
        DeepLinkHandler.onDeepLink("https://example.com/cached")

        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        assertEquals("https://example.com/cached", received)
    }

    @Test
    fun settingListenerClearsCacheAfterDelivery() {
        DeepLinkHandler.onDeepLink("https://example.com/first")

        var callCount = 0
        DeepLinkHandler.listener = { callCount++ }
        assertEquals(1, callCount)

        // Setting a new listener should NOT re-deliver the old cached URI
        var secondReceived: String? = null
        DeepLinkHandler.listener = { secondReceived = it }

        assertNull(secondReceived)
    }

    @Test
    fun onlyLastCachedUriIsDelivered() {
        DeepLinkHandler.onDeepLink("https://example.com/first")
        DeepLinkHandler.onDeepLink("https://example.com/second")

        var received: String? = null
        DeepLinkHandler.listener = { received = it }

        assertEquals("https://example.com/second", received)
    }

    @Test
    fun noCallbackWhenListenerIsNull() {
        DeepLinkHandler.listener = null

        // Should not throw
        DeepLinkHandler.onDeepLink("https://example.com/test")
    }
}

