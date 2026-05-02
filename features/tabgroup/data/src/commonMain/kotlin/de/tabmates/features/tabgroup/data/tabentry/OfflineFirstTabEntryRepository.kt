package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single(binds = [TabEntryRepository::class])
class OfflineFirstTabEntryRepository(
    private val service: TabEntryService,
    private val database: TabMatesDatabase,
) : TabEntryRepository {
    override fun getTabEntries(groupId: String): Flow<List<TabEntry>> =
        database.tabEntryDao
            .getTabEntriesByGroupId(groupId)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun fetchTabEntries(
        groupId: String,
        before: Instant?,
        pageSize: Int,
    ): Result<List<TabEntry>, DataError.Remote> =
        service
            .getTabEntriesForGroup(groupId, before, pageSize)
            .onSuccess { entries ->
                persist(
                    entries = entries,
                    groupId = groupId,
                    shouldSync = before == null,
                    pageSize = pageSize,
                )
            }

    private suspend fun persist(
        entries: List<TabEntry>,
        groupId: String,
        shouldSync: Boolean,
        pageSize: Int,
    ) {
        val entryEntities = entries.map { it.toEntity() }
        val splitsByEntryId =
            entries.associate { entry ->
                entry.tabEntryId to
                    when (entry) {
                        is TabEntry.Expense -> entry.splits.map { it.toEntity() }
                        is TabEntry.Income -> entry.splits.map { it.toEntity() }
                        is TabEntry.Settlement -> emptyList()
                    }
            }

        database.tabEntryDao.upsertTabEntriesAndSyncIfNecessary(
            groupId = groupId,
            entries = entryEntities,
            splitsByEntryId = splitsByEntryId,
            splitDao = database.tabEntrySplitDao,
            pageSize = pageSize,
            shouldSync = shouldSync,
        )
    }
}
