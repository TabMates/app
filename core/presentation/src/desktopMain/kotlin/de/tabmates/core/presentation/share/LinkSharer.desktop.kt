package de.tabmates.core.presentation.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberLinkSharer(): LinkSharer =
    remember {
        LinkSharer { link ->
            val selection = StringSelection(link)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            LinkShareResult.Copied
        }
    }
