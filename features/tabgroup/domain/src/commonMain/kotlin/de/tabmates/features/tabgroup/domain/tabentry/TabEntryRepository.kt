package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface TabEntryRepository {
    fun getTabEntriesForGroup(groupId: String): Flow<List<TabEntry>>

    suspend fun fetchTabEntries(
        groupId: String,
        before: Instant? = null,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<List<TabEntry>, DataError.Remote>

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
