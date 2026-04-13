package de.tabmates.composeapp.deeplink

/**
 * Platform-bridge singleton that receives deep-link URIs from each platform
 * (Android Intent, iOS onOpenURL, Desktop URI handler, Web location) and
 * forwards them to a registered [listener].
 *
 * If a URI arrives before a listener is set, it is cached and delivered
 * as soon as a listener is registered.
 *
 * Compose-side consumption is handled by [HandleDeepLinks].
 */
object DeepLinkHandler {
    private var cached: String? = null

    var listener: ((uri: String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cached?.let { value.invoke(it) }
                cached = null
            }
        }

    /** Called by platform entry points when a deep-link URI arrives. */
    fun onDeepLink(uri: String) {
        cached = uri
        listener?.let {
            it.invoke(uri)
            cached = null
        }
    }
}

