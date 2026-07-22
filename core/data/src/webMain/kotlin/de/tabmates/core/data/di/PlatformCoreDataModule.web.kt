package de.tabmates.core.data.di

import de.tabmates.core.data.biometric.UnsupportedBiometricAuthenticator
import de.tabmates.core.data.security.WebKSafeInstances
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformCoreDataModule {
    @Single
    fun provideHttpClientEngine(): HttpClientEngine = Js.create()

    @Single
    fun provideBiometricAuthenticator(): BiometricAuthenticator = UnsupportedBiometricAuthenticator()

    // The instances pre-warmed by awaitSecureStorageReady() at startup — see
    // WebSecureStorage.kt for why web must not construct KSafe lazily here.
    @Single
    @Named("prefs")
    fun providePrefsKSafe(): KSafe = WebKSafeInstances.prefs

    @Single
    @Named("vault")
    fun provideVaultKSafe(): KSafe = WebKSafeInstances.vault
}
