package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.NewExpenseSplit
import de.tabmates.features.tabgroup.domain.tabentry.SplitResolver
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Single(binds = [TabEntryRepository::class])
class OfflineFirstTabEntryRepository(
    private val service: TabEntryService,
    private val database: TabMatesDatabase,
    private val outbox: TabEntryOutbox,
) : TabEntryRepository {
    override fun getTabEntriesForGroup(groupId: String): Flow<List<TabEntry>> =
        database.tabEntryDao
            .getTabEntriesByGroupId(groupId)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getTabEntryById(tabEntryId: String): Flow<TabEntry?> =
        database.tabEntrySplitDao
            .getSplitsByTabEntryId(tabEntryId)
            .map { _ -> database.tabEntryDao.getTabEntryById(tabEntryId)?.toDomain() }

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
        val localId = generateLocalId()
        val now = Clock.System.now()
        val resolvedSplits = resolveSplits(localId, splits, amount)
        val expense =
            TabEntry.Expense(
                tabEntryId = localId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                createdAt = createdAt,
                lastModifiedAt = now,
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                splits = resolvedSplits,
            )

        insertLocal(expense)
        outbox.enqueueCreateExpense(
            clientRequestId = localId,
            groupId = groupId,
            title = title,
            description = description,
            amount = amount,
            currencyCode = currencyCode,
            paidByUserId = paidByUserId,
            createdAt = createdAt,
            splits = splits,
        )
        return Result.Success(expense)
    }

    override suspend fun updateExpense(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        createdAt: Instant,
        splits: List<NewExpenseSplit>,
    ): Result<TabEntry.Expense, DataError.Remote> {
        // Preserve server-owned fields from the existing row; the server reconciles the rest on echo.
        val existing = database.tabEntryDao.getTabEntryById(tabEntryId)?.toDomain() as? TabEntry.Expense
        val now = Clock.System.now()
        val resolvedSplits = resolveSplits(tabEntryId, splits, amount)
        val expense =
            TabEntry.Expense(
                tabEntryId = tabEntryId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                creatorId = existing?.creatorId ?: paidByUserId,
                paidByUserId = paidByUserId,
                createdAt = createdAt,
                lastModifiedAt = now,
                lastModifiedByUserId = paidByUserId,
                version = existing?.version ?: 0,
                deletedAt = null,
                deletedByUserId = null,
                splits = resolvedSplits,
            )

        database.tabEntryDao.replaceTabEntryWithSplits(
            entry = expense.toEntity(pendingSync = true),
            splits = resolvedSplits.map { it.toEntity() },
            splitDao = database.tabEntrySplitDao,
        )
        outbox.enqueueUpdateExpense(
            tabEntryId = tabEntryId,
            groupId = groupId,
            title = title,
            description = description,
            amount = amount,
            currencyCode = currencyCode,
            paidByUserId = paidByUserId,
            createdAt = createdAt,
            splits = splits,
        )
        return Result.Success(expense)
    }

    override suspend fun createSettlement(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        paidByUserId: String,
        receivedByUserId: String,
        createdAt: Instant,
    ): Result<TabEntry.Settlement, DataError.Remote> {
        val localId = generateLocalId()
        val now = Clock.System.now()
        val settlement =
            TabEntry.Settlement(
                tabEntryId = localId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currencyCode = currencyCode,
                creatorId = paidByUserId,
                paidByUserId = paidByUserId,
                createdAt = createdAt,
                lastModifiedAt = now,
                lastModifiedByUserId = paidByUserId,
                version = 0,
                deletedAt = null,
                deletedByUserId = null,
                receivedByUserId = receivedByUserId,
            )

        database.tabEntryDao.upsertTabEntry(settlement.toEntity(pendingSync = true))
        outbox.enqueueCreateSettlement(
            clientRequestId = localId,
            groupId = groupId,
            title = title,
            description = description,
            amount = amount,
            currencyCode = currencyCode,
            paidByUserId = paidByUserId,
            receivedByUserId = receivedByUserId,
            createdAt = createdAt,
        )
        return Result.Success(settlement)
    }

    override suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote> {
        // Persist intent first so a crash between enqueue and local delete still drives the
        // remote delete on next drain. Outbox upsert by id is idempotent.
        outbox.enqueueDeleteTabEntry(tabEntryId)
        database.tabEntryDao.deleteTabEntryAndSplits(
            tabEntryId = tabEntryId,
            splitDao = database.tabEntrySplitDao,
        )
        return Result.Success(Unit)
    }

    private suspend fun insertLocal(expense: TabEntry.Expense) {
        database.tabEntryDao.upsertTabEntry(expense.toEntity(pendingSync = true))
        if (expense.splits.isNotEmpty()) {
            database.tabEntrySplitDao.upsertSplits(expense.splits.map { it.toEntity() })
        }
    }

    private fun resolveSplits(
        tabEntryId: String,
        splits: List<NewExpenseSplit>,
        totalAmount: Double,
    ): List<TabEntrySplit> {
        if (splits.isEmpty()) return emptyList()
        val resolvedAmounts = SplitResolver.resolveAmounts(splits, totalAmount)
        return splits.mapIndexed { index, split ->
            TabEntrySplit(
                splitId = generateLocalId(),
                tabEntryId = tabEntryId,
                participantId = split.participantId,
                splitType = split.splitType,
                value = split.value,
                resolvedAmount = resolvedAmounts[index],
            )
        }
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
                        is TabEntry.Settlement -> emptyList<TabEntrySplitEntity>()
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

    @OptIn(ExperimentalUuidApi::class)
    private fun generateLocalId(): String = Uuid.random().toString()
}
