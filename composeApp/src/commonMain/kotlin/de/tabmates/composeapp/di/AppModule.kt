package de.tabmates.composeapp.di

import de.tabmates.composeapp.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule =
    module {
        viewModelOf(::MainViewModel)
    }

