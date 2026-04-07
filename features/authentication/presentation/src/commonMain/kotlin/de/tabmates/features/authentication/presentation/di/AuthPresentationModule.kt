package de.tabmates.features.authentication.presentation.di

import de.tabmates.features.authentication.presentation.register.RegisterViewModel
import de.tabmates.features.authentication.presentation.registersuccess.RegisterSuccessViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule =
    module {
        viewModelOf(::RegisterViewModel)
        viewModel<RegisterSuccessViewModel> { (email: String) ->
            RegisterSuccessViewModel(get(), email)
        }
    }
