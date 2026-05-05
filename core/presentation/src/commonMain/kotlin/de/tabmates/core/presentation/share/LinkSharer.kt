package de.tabmates.core.presentation.share

import androidx.compose.runtime.Composable

enum class LinkShareResult {
    Copied,
    Shared,
}

fun interface LinkSharer {
    fun share(link: String): LinkShareResult
}

@Composable
expect fun rememberLinkSharer(): LinkSharer
