package de.tabmates.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/** Local, device-scoped user preferences (theme, notifications). Not synced to the server. */
interface AppPreferencesRepository {
    fun themeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    fun notificationsEnabled(): Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)
}
