package de.tabmates.core.domain.preferences

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** Local, device-scoped user preferences (theme, notifications, language). Not synced to the server. */
interface AppPreferencesRepository {
    fun themeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    fun notificationsEnabled(): Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    fun appLanguage(): Flow<AppLanguage>

    suspend fun setAppLanguage(language: AppLanguage)

    /**
     * When the "get the Android app" promo may reappear, or `null` if it was never dismissed.
     * Only the web client reads this; every other platform is already the app.
     */
    fun androidAppPromoSnoozedUntil(): Flow<Instant?>

    suspend fun snoozeAndroidAppPromo(until: Instant)

    /**
     * When currencies and exchange rates were last pulled, or `null` if never. Read as a plain
     * value rather than a [Flow]: the only reader compares it against the refresh interval once
     * per sign-in, and the environment switch clears it so the next backend is pulled fresh.
     */
    suspend fun lastCurrencySync(): Instant?

    suspend fun setLastCurrencySync(instant: Instant?)
}
