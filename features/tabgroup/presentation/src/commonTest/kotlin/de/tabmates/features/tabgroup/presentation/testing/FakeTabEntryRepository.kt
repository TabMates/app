package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.NewExpenseSplit
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class FakeTabEntryRepository(
    initialEntries: Map<String, List<TabEntry>> = emptyMap(),
) : TabEntryRepository {
    private val flowByGroupId: MutableMap<String, MutableStateFlow<List<TabEntry>>> =
        initialEntries.mapValues { MutableStateFlow(it.value) }.toMutableMap()

    /** When set, [createSettlement]/[updateSettlement] fail with this error instead of writing. */
    var settlementError: DataError.Remote? = null

    /** When set, [createSettlement] suspends until the deferred completes (for in-flight tests). */
    var settlementGate: CompletableDeferred<Unit>? = null

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

    override suspend fun createExpense(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
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
                exchangeRate = exchangeRate,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                entryDate = entryDate,
                createdAt = Clock.System.now(),
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

    override suspend fun updateExpense(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewExpenseSplit>,
    ): Result<TabEntry.Expense, DataError.Remote> {
        val expense =
            TabEntry.Expense(
                tabEntryId = tabEntryId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                exchangeRate = exchangeRate,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                entryDate = entryDate,
                createdAt = Clock.System.now(),
                lastModifiedAt = Clock.System.now(),
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                splits =
                    splits.mapIndexed { index, split ->
                        TabEntrySplit(
                            splitId = "$tabEntryId-split-$index",
                            tabEntryId = tabEntryId,
                            participantId = split.participantId,
                            splitType = split.splitType,
                            value = split.value,
                            resolvedAmount = split.value,
                        )
                    },
            )
        val flow = flowByGroupId.getOrPut(groupId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value.map { if (it.tabEntryId == tabEntryId) expense else it }
        return Result.Success(expense)
    }

    override suspend fun createSettlement(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
    ): Result<TabEntry.Settlement, DataError.Remote> {
        settlementGate?.await()
        settlementError?.let { return Result.Failure(it) }
        val id = "fake-${flowByGroupId.values.sumOf { it.value.size } + 1}"
        val settlement =
            TabEntry.Settlement(
                tabEntryId = id,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                exchangeRate = exchangeRate,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                entryDate = entryDate,
                createdAt = Clock.System.now(),
                lastModifiedAt = Clock.System.now(),
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                receivedByUserId = receivedByUserId,
            )
        val flow = flowByGroupId.getOrPut(groupId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + settlement
        return Result.Success(settlement)
    }

    override suspend fun updateSettlement(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
    ): Result<TabEntry.Settlement, DataError.Remote> {
        settlementError?.let { return Result.Failure(it) }
        val settlement =
            TabEntry.Settlement(
                tabEntryId = tabEntryId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                exchangeRate = exchangeRate,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                entryDate = entryDate,
                createdAt = Clock.System.now(),
                lastModifiedAt = Clock.System.now(),
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                receivedByUserId = receivedByUserId,
            )
        val flow = flowByGroupId.getOrPut(groupId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value.map { if (it.tabEntryId == tabEntryId) settlement else it }
        return Result.Success(settlement)
    }

    override suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote> {
        flowByGroupId.values.forEach { flow ->
            flow.value = flow.value.filterNot { it.tabEntryId == tabEntryId }
        }
        return Result.Success(Unit)
    }
}
