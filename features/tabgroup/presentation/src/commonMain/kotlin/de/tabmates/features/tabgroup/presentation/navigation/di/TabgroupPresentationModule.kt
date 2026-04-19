package de.tabmates.features.tabgroup.presentation.navigation.di

import de.tabmates.features.tabgroup.presentation.navigation.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val tabgroupPresentationModule =
    module {
        viewModelOf(::HomeViewModel)
    }
