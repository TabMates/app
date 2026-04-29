package de.tabmates.features.tabgroup.data.di

import de.tabmates.features.tabgroup.data.group.KtorGroupService
import de.tabmates.features.tabgroup.data.group.OfflineFirstGroupRepository
import de.tabmates.features.tabgroup.data.tabentry.KtorTabEntryService
import de.tabmates.features.tabgroup.data.tabentry.OfflineFirstTabEntryRepository
import de.tabmates.features.tabgroup.data.tabentry.TabEntryService
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformTabgroupDataModule: Module

val tabgroupDataModule =
    module {
        includes(platformTabgroupDataModule)

        singleOf(::KtorGroupService) bind GroupService::class
        singleOf(::OfflineFirstGroupRepository) bind GroupRepository::class
        singleOf(::KtorTabEntryService) bind TabEntryService::class
        singleOf(::OfflineFirstTabEntryRepository) bind TabEntryRepository::class
    }
