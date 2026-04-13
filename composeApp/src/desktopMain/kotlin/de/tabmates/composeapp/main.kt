package de.tabmates.composeapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.tabmates.composeapp.deeplink.DeepLinkHandler
import java.awt.Desktop

fun main(args: Array<String>) {
    // Handle deep links received as command-line arguments (cold start)
    args.firstOrNull { it.startsWith("https://") || it.startsWith("tabmates://") }
        ?.let { DeepLinkHandler.onDeepLink(it) }

    // Handle deep links when the app is already running (macOS)
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_URI)) {
        Desktop.getDesktop().setOpenURIHandler { event ->
            DeepLinkHandler.onDeepLink(event.uri.toString())
        }
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TabMates",
        ) {
            App()
        }
    }
}
