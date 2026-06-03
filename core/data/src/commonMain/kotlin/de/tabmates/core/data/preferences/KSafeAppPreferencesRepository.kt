package de.tabmates.core.data.preferences

import de.tabmates.core.domain.preferences.AppLanguage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single(binds = [AppPreferencesRepository::class])
class KSafeAppPreferencesRepository(
    @Named("prefs") private val prefs: KSafe,
) : AppPreferencesRepository {
    override fun themeMode(): Flow<ThemeMode> =
        prefs.getFlow(KEY_THEME_MODE, ThemeMode.SYSTEM.name).map { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.put(KEY_THEME_MODE, mode.name, KSafeWriteMode.Plain)
    }

    override fun notificationsEnabled(): Flow<Boolean> = prefs.getFlow(KEY_NOTIFICATIONS, true)

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefs.put(KEY_NOTIFICATIONS, enabled, KSafeWriteMode.Plain)
    }

    override fun appLanguage(): Flow<AppLanguage> =
        prefs.getFlow(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.name).map { stored ->
            runCatching { AppLanguage.valueOf(stored) }.getOrDefault(AppLanguage.SYSTEM)
        }

    override suspend fun setAppLanguage(language: AppLanguage) {
        prefs.put(KEY_APP_LANGUAGE, language.name, KSafeWriteMode.Plain)
    }

    private companion object {
        private const val KEY_THEME_MODE = "themeMode"
        private const val KEY_NOTIFICATIONS = "notificationsEnabled"
        private const val KEY_APP_LANGUAGE = "appLanguage"
    }
}
