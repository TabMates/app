package de.tabmates.core.data.preferences

import java.util.Locale

actual fun deviceLanguageTag(): String = Locale.getDefault().toLanguageTag()
