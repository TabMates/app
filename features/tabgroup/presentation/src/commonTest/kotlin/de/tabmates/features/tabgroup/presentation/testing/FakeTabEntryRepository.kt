package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.NewExpenseSplit
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
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

    override fun getTabEntryById(tabEntryId: String): Flow<TabEntry?> =
        flowByGroupId.values
            .firstOrNull { flow -> flow.value.any { it.tabEntryId == tabEntryId } }
            ?.map { entries -> entries.firstOrNull { it.tabEntryId == tabEntryId } }
            ?: MutableStateFlow<TabEntry?>(null)

    override suspend fun fetchTabEntries(
        groupId: String,
        before: Instant?,
        pageSize: Int,
    ): Result<List<TabEntry>, DataError.Remote> = Result.Success(flowByGroupId[groupId]?.value.orEmpty())

    override suspend fun createExpense(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        createdAt: Instant,
        splits: List<NewExpenseSplit>,
    ): Result<TabEntry.Expense, DataError.Remote> {
        val id = "fake-${flowByGroupId.values.sumOf { it.value.size } + 1}"
        val expense =
            TabEntry.Expense(
                tabEntryId = id,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                createdAt = createdAt,
                lastModifiedAt = Clock.System.now(),
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                splits =
                    splits.mapIndexed { index, split ->
                        TabEntrySplit(
                            splitId = "$id-split-$index",
                            tabEntryId = id,
                            participantId = split.participantId,
                            splitType = split.splitType,
                            value = split.value,
                            resolvedAmount = split.value,
                        )
                    },
            )
        val flow = flowByGroupId.getOrPut(groupId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + expense
        return Result.Success(expense)
    }

    override suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote> {
        flowByGroupId.values.forEach { flow ->
            flow.value = flow.value.filterNot { it.tabEntryId == tabEntryId }
        }
        return Result.Success(Unit)
    }
}
