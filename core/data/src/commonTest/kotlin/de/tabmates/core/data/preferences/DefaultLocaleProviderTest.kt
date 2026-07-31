package de.tabmates.core.data.preferences

import de.tabmates.core.domain.preferences.AppLanguage
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DefaultLocaleProviderTest {
    private class FakeAppPreferencesRepository(
        private val language: AppLanguage,
    ) : AppPreferencesRepository {
        override fun themeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

        override suspend fun setThemeMode(mode: ThemeMode) = Unit

        override fun notificationsEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit

        override fun appLanguage(): Flow<AppLanguage> = flowOf(language)

        override suspend fun setAppLanguage(language: AppLanguage) = Unit

        override fun androidAppPromoSnoozedUntil(): Flow<Instant?> = flowOf(null)

        override suspend fun snoozeAndroidAppPromo(until: Instant) = Unit
    }

    @Test
    fun pinnedLanguage_usesItsTag() =
        runTest {
            val provider = DefaultLocaleProvider(FakeAppPreferencesRepository(AppLanguage.GERMAN))

            assertEquals("de", provider.currentLanguageTag())
        }

    @Test
    fun systemLanguage_fallsBackToDeviceLocale() =
        runTest {
            val provider = DefaultLocaleProvider(FakeAppPreferencesRepository(AppLanguage.SYSTEM))

            // SYSTEM has no pinned tag -> device locale; exact value is platform-dependent, but non-blank.
            assertTrue(provider.currentLanguageTag().isNotBlank())
        }
}
