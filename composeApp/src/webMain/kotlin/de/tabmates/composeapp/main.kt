package de.tabmates.composeapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.tabmates.composeapp.deeplink.DeepLinkHandler
import de.tabmates.core.data.security.awaitSecureStorageReady
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val currentUrl = window.location.href
    DeepLinkHandler.onDeepLink(currentUrl)

    // KSafe decryption is async-only on web; starting the UI before its caches are warm
    // makes the stored session read as null and logs the user out on every cold start.
    // index.html keeps its loading spinner up until the first frame.
    MainScope().launch {
        awaitSecureStorageReady()
        ComposeViewport {
            App()
        }
    }
}
