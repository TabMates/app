package de.tabmates.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Resolves the language the app should currently use: the in-app [AppLanguage] when one is
 * pinned, otherwise the device locale. Returns BCP-47 tags (e.g. "en", "de"). Used both for
 * UI localization and to tell the backend which language to localize push notifications in.
 */
interface LocaleProvider {
    /** The current language tag, then a new value whenever the in-app language changes. */
    fun languageTag(): Flow<String>

    /** The current language tag, resolved once. */
    suspend fun currentLanguageTag(): String
}
