package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.preferences.AppLanguage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

class FakeAppPreferencesRepository(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialNotificationsEnabled: Boolean = true,
    initialLanguage: AppLanguage = AppLanguage.SYSTEM,
    initialAndroidAppPromoSnoozedUntil: Instant? = null,
) : AppPreferencesRepository {
    private val themeMode = MutableStateFlow(initialThemeMode)
    private val notificationsEnabled = MutableStateFlow(initialNotificationsEnabled)
    private val language = MutableStateFlow(initialLanguage)
    private val androidAppPromoSnoozedUntil = MutableStateFlow(initialAndroidAppPromoSnoozedUntil)

    val setThemeModeCalls: MutableList<ThemeMode> = mutableListOf()
    val setNotificationsEnabledCalls: MutableList<Boolean> = mutableListOf()
    val setAppLanguageCalls: MutableList<AppLanguage> = mutableListOf()
    val snoozeAndroidAppPromoCalls: MutableList<Instant> = mutableListOf()

    override fun themeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        setThemeModeCalls += mode
        themeMode.value = mode
    }

    override fun notificationsEnabled(): Flow<Boolean> = notificationsEnabled

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        setNotificationsEnabledCalls += enabled
        notificationsEnabled.value = enabled
    }

    override fun appLanguage(): Flow<AppLanguage> = language

    override suspend fun setAppLanguage(language: AppLanguage) {
        setAppLanguageCalls += language
        this.language.value = language
    }

    override fun androidAppPromoSnoozedUntil(): Flow<Instant?> = androidAppPromoSnoozedUntil

    override suspend fun snoozeAndroidAppPromo(until: Instant) {
        snoozeAndroidAppPromoCalls += until
        androidAppPromoSnoozedUntil.value = until
    }
}
