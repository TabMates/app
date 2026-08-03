package de.tabmates.core.presentation.di

import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.core.presentation.format.platformNumberSymbols
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("de.tabmates.core.presentation")
class CorePresentationModule {
    /**
     * Read once at startup. ViewModels that seed amount text fields need the same [NumberSymbols]
     * the UI formats with, and taking it as a dependency keeps their tests off the device locale.
     */
    @Single
    fun provideNumberSymbols(): NumberSymbols = platformNumberSymbols()
}
