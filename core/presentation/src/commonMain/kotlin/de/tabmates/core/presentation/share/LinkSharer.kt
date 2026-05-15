package de.tabmates.core.presentation.share

import androidx.compose.runtime.Composable

enum class LinkShareResult {
    Copied,
    Shared,
}

interface LinkSharer {
    fun copy(link: String): LinkShareResult

    fun share(link: String): LinkShareResult
}

@Composable
expect fun rememberLinkSharer(): LinkSharer
