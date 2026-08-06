package de.tabmates.features.tabgroup.data.di

import de.tabmates.features.tabgroup.domain.group.GroupRemovalNotifier
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("de.tabmates.features.tabgroup.data")
class TabgroupDataModule {
    // Lives in the domain module, which carries no Koin annotations — same arrangement as
    // UpgradeRequiredNotifier, which CoreDataModule provides for the same reason.
    @Single
    fun provideGroupRemovalNotifier(): GroupRemovalNotifier = GroupRemovalNotifier()
}
