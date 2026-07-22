package de.tabmates.core.data.di

import de.tabmates.core.data.biometric.IosBiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformCoreDataModule {
    @Single
    fun provideHttpClientEngine(): HttpClientEngine = Darwin.create()

    @Single
    fun provideBiometricAuthenticator(): BiometricAuthenticator = IosBiometricAuthenticator()

    @Single
    @Named("prefs")
    fun providePrefsKSafe(): KSafe =
        KSafe(
            fileName = "prefs",
        )

    @Single
    @Named("vault")
    fun provideVaultKSafe(): KSafe = KSafe(fileName = "vault")
}
