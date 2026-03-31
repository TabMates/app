package de.tabmates.features.tabgroup.data.di

import de.tabmates.features.tabgroup.data.group.KtorGroupService
import de.tabmates.features.tabgroup.domain.group.GroupService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val tabgroupDataModule =
    module {
        singleOf(::KtorGroupService) bind GroupService::class
    }
