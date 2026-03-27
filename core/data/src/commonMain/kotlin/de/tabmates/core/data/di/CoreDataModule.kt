package de.tabmates.core.data.di

import de.tabmates.core.data.logging.KermitLogger
import de.tabmates.core.data.networking.HttpClientFactory
import de.tabmates.core.domain.logging.TabMatesLogger
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule =
    module {
        includes(platformCoreDataModule)

        singleOf(::KermitLogger) bind TabMatesLogger::class
        single {
            HttpClientFactory(get()).create(get())
        }
    }
