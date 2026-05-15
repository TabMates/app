package de.tabmates.core.presentation.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberLinkSharer(): LinkSharer =
    remember {
        object : LinkSharer {
            override fun copy(link: String): LinkShareResult {
                UIPasteboard.generalPasteboard.string = link
                return LinkShareResult.Copied
            }

            override fun share(link: String): LinkShareResult {
                val controller =
                    UIActivityViewController(
                        activityItems = listOf(link),
                        applicationActivities = null,
                    )
                UIApplication.sharedApplication.keyWindow
                    ?.rootViewController
                    ?.presentViewController(controller, animated = true, completion = null)
                return LinkShareResult.Shared
            }
        }
    }
