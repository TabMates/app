package de.tabmates.features.authentication.data.di

import de.tabmates.features.authentication.data.KtorAuthService
import de.tabmates.features.authentication.domain.AuthService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val authenticationDataModule =
    module {
        singleOf(::KtorAuthService) bind AuthService::class
    }
