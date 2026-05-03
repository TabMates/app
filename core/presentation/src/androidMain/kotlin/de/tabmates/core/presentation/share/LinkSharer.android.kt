package de.tabmates.core.presentation.share

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberLinkSharer(): LinkSharer {
    val context = LocalContext.current
    return remember(context) {
        LinkSharer { link ->
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
            LinkShareResult.Shared
        }
    }
}
