package de.tabmates.core.presentation.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

@Composable
actual fun rememberLinkSharer(): LinkSharer {
    val context = LocalContext.current
    return remember(context) {
        object : LinkSharer {
            override fun copy(link: String): LinkShareResult {
                val clipboard = context.getSystemService<ClipboardManager>()
                clipboard?.setPrimaryClip(ClipData.newPlainText("invite", link))
                return LinkShareResult.Copied
            }

            override fun share(link: String): LinkShareResult {
                val sendIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                val chooser =
                    Intent.createChooser(sendIntent, null).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(chooser)
                return LinkShareResult.Shared
            }
        }
    }
}
