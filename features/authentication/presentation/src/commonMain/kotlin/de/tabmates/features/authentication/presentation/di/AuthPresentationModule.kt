package de.tabmates.features.authentication.presentation.di

import de.tabmates.features.authentication.presentation.emailverification.EmailVerificationViewModel
import de.tabmates.features.authentication.presentation.login.LoginViewModel
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
        viewModel<EmailVerificationViewModel> { (token: String) ->
            EmailVerificationViewModel(get(), token)
        }
        viewModelOf(::LoginViewModel)
    }
