package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

class FakeTabEntryRepository(
    initialEntries: Map<String, List<TabEntry>> = emptyMap(),
) : TabEntryRepository {
    private val flowByGroupId: MutableMap<String, MutableStateFlow<List<TabEntry>>> =
        initialEntries.mapValues { MutableStateFlow(it.value) }.toMutableMap()

    fun emit(
        groupId: String,
        entries: List<TabEntry>,
    ) {
        flowByGroupId
            .getOrPut(groupId) { MutableStateFlow(emptyList()) }
            .value = entries
    }

    override fun getTabEntriesForGroup(groupId: String): Flow<List<TabEntry>> =
        flowByGroupId.getOrPut(groupId) { MutableStateFlow(emptyList()) }

    override suspend fun fetchTabEntries(
        groupId: String,
        before: Instant?,
        pageSize: Int,
    ): Result<List<TabEntry>, DataError.Remote> = Result.Success(flowByGroupId[groupId]?.value.orEmpty())
}
