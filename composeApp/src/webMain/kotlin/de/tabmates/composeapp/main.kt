package de.tabmates.composeapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.tabmates.composeapp.deeplink.DeepLinkHandler
import de.tabmates.core.data.security.awaitSecureStorageReady
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// Must match the sessionStorage key written by 404.html.
private const val DEEP_LINK_STASH_KEY = "tm_deeplink"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // A deep path (e.g. /j/<token>) has no file on static GitHub Pages, so 404.html stashed the
    // original URL and bounced us to the root. Prefer that stashed URL (consumed once); otherwise
    // use the address bar as usual. Either way the shared DeepLinkHandler resolves it.
    val stashedDeepLink =
        window.sessionStorage.getItem(DEEP_LINK_STASH_KEY)
            ?.also { window.sessionStorage.removeItem(DEEP_LINK_STASH_KEY) }
    DeepLinkHandler.onDeepLink(stashedDeepLink ?: window.location.href)

    // KSafe decryption is async-only on web; starting the UI before its caches are warm
    // makes the stored session read as null and logs the user out on every cold start.
    // index.html keeps its loading spinner up until the first frame.
    MainScope().launch {
        awaitSecureStorageReady()
        // Mounts into document.body and clears its children, so nothing declared in index.html's
        // body survives this call (turnstile.js therefore creates its container from JS).
        ComposeViewport {
            App()
        }
    }
}
