package de.tabmates.core.presentation.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberLinkSharer(): LinkSharer =
    remember {
        object : LinkSharer {
            override fun copy(link: String): LinkShareResult {
                writeToClipboard(link)
                return LinkShareResult.Copied
            }

            override fun share(link: String): LinkShareResult {
                writeToClipboard(link)
                return LinkShareResult.Copied
            }

            private fun writeToClipboard(text: String) {
                val selection = StringSelection(text)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            }
        }
    }
