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
import kotlin.time.Instant

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

    // KSafe stores primitives, so the instant lives as epoch millis with 0 standing in for
    // "never dismissed" — a real snooze is always far in the future, never the epoch.
    override fun androidAppPromoSnoozedUntil(): Flow<Instant?> =
        prefs.getFlow(KEY_APP_PROMO_SNOOZED_UNTIL, NEVER_SNOOZED).map { millis ->
            millis.takeIf { it != NEVER_SNOOZED }?.let(Instant::fromEpochMilliseconds)
        }

    override suspend fun snoozeAndroidAppPromo(until: Instant) {
        prefs.put(KEY_APP_PROMO_SNOOZED_UNTIL, until.toEpochMilliseconds(), KSafeWriteMode.Plain)
    }

    // Same epoch-millis-with-0-for-never encoding as the promo snooze above.
    override suspend fun lastCurrencySync(): Instant? =
        prefs
            .get(KEY_LAST_CURRENCY_SYNC, NEVER_SYNCED)
            .takeIf { it != NEVER_SYNCED }
            ?.let(Instant::fromEpochMilliseconds)

    override suspend fun setLastCurrencySync(instant: Instant?) {
        prefs.put(KEY_LAST_CURRENCY_SYNC, instant?.toEpochMilliseconds() ?: NEVER_SYNCED, KSafeWriteMode.Plain)
    }

    private companion object {
        private const val KEY_THEME_MODE = "themeMode"
        private const val KEY_NOTIFICATIONS = "notificationsEnabled"
        private const val KEY_APP_LANGUAGE = "appLanguage"
        private const val KEY_APP_PROMO_SNOOZED_UNTIL = "androidAppPromoSnoozedUntil"
        private const val NEVER_SNOOZED = 0L

        // Key kept as-is: CurrencySyncCoordinator wrote it under this name before the stamp moved
        // here, so existing installs keep their last sync time instead of refetching once.
        private const val KEY_LAST_CURRENCY_SYNC = "lastCurrencySync"
        private const val NEVER_SYNCED = 0L
    }
}
