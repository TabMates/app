package de.tabmates.core.data.di

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
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
    @Named("prefs")
    fun providePrefsKSafe(): KSafe =
        KSafe(
            fileName = "prefs",
            memoryPolicy = KSafeMemoryPolicy.PLAIN_TEXT,
        )

    @Single
    @Named("vault")
    fun provideVaultKSafe(): KSafe = KSafe(fileName = "vault")
}
