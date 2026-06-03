package de.tabmates.core.data.preferences

import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.LocaleProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * Resolves the active language: the pinned in-app language if set, otherwise the device locale.
 */
@Single(binds = [LocaleProvider::class])
class DefaultLocaleProvider(
    private val preferences: AppPreferencesRepository,
) : LocaleProvider {
    override fun languageTag(): Flow<String> =
        preferences.appLanguage().map { language ->
            language.languageTag ?: deviceLanguageTag()
        }

    override suspend fun currentLanguageTag(): String = languageTag().first()
}
