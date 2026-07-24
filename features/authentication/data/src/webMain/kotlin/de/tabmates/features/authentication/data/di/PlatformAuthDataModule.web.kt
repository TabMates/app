package de.tabmates.features.authentication.data.di

import de.tabmates.features.authentication.data.WebTurnstileTokenProvider
import de.tabmates.features.authentication.domain.TurnstileTokenProvider
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformAuthDataModule {
    @Single
    fun provideTurnstileTokenProvider(): TurnstileTokenProvider = WebTurnstileTokenProvider()
}
