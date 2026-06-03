package de.tabmates.core.data.preferences

import kotlinx.browser.window

actual fun deviceLanguageTag(): String = window.navigator.language.ifBlank { "en" }
