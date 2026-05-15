package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlin.time.Instant

interface TabEntryService {
    suspend fun getTabEntriesForGroup(
        groupId: String,
        before: Instant?,
        pageSize: Int,
    ): Result<List<TabEntry>, DataError.Remote>
}
