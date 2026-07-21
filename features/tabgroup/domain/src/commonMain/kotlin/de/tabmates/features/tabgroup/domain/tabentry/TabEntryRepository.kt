package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TabEntryRepository {
    fun getTabEntriesForGroup(groupId: String): Flow<List<TabEntry>>

    fun getTabEntryById(tabEntryId: String): Flow<TabEntry?>

    suspend fun createExpense(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
    ): Result<TabEntry.Expense, DataError.Remote>

    suspend fun updateExpense(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
    ): Result<TabEntry.Expense, DataError.Remote>

    suspend fun createIncome(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
    ): Result<TabEntry.Income, DataError.Remote>

    suspend fun updateIncome(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
    ): Result<TabEntry.Income, DataError.Remote>

    suspend fun createSettlement(
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
    ): Result<TabEntry.Settlement, DataError.Remote>

    suspend fun updateSettlement(
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
    ): Result<TabEntry.Settlement, DataError.Remote>

    suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote>
}
