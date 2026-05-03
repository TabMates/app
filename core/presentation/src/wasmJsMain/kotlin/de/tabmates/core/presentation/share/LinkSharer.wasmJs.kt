@file:OptIn(ExperimentalWasmJsInterop::class)

package de.tabmates.core.presentation.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@Composable
actual fun rememberLinkSharer(): LinkSharer =
    remember {
        LinkSharer { link ->
            writeTextToClipboard(link)
            LinkShareResult.Copied
        }
    }

@Suppress("UNUSED_PARAMETER")
private fun writeTextToClipboard(text: String): JsAny? = js("navigator.clipboard.writeText(text)")
