package de.tabmates.core.presentation.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberLinkSharer(): LinkSharer =
    remember {
        LinkSharer { link ->
            val controller =
                UIActivityViewController(
                    activityItems = listOf(link),
                    applicationActivities = null,
                )
            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.presentViewController(controller, animated = true, completion = null)
            LinkShareResult.Shared
        }
    }
