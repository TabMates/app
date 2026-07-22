package de.tabmates.core.data.di

import android.content.Context
import de.tabmates.core.data.biometric.AndroidBiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformCoreDataModule {
    @Single
    fun provideHttpClientEngine(): HttpClientEngine = OkHttp.create()

    @Single
    fun provideBiometricAuthenticator(context: Context): BiometricAuthenticator =
        AndroidBiometricAuthenticator(context)

    @Single
    @Named("prefs")
    fun providePrefsKSafe(context: Context): KSafe =
        KSafe(
            context = context,
            fileName = "prefs",
        )

    @Single
    @Named("vault")
    fun provideVaultKSafe(context: Context): KSafe =
        KSafe(
            context = context,
            fileName = "vault",
        )
}
