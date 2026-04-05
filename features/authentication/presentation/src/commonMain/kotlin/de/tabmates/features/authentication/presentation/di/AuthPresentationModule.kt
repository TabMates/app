package de.tabmates.features.authentication.presentation.di

import de.tabmates.features.authentication.presentation.register.RegisterViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule =
    module {
        viewModelOf(::RegisterViewModel)
    }
