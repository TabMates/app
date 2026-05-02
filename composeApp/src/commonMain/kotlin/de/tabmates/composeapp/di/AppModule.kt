package de.tabmates.composeapp.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan("de.tabmates.composeapp")
class AppModule

@KoinApplication
class TabMatesKoinApp
