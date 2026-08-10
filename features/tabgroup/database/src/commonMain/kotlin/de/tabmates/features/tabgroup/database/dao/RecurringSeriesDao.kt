package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.RecurringExceptionEntity
import de.tabmates.features.tabgroup.database.entities.RecurringSeriesEntity
import de.tabmates.features.tabgroup.database.entities.RecurringSeriesWithDetails
import de.tabmates.features.tabgroup.database.entities.RecurringTemplateSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringSeriesDao {
    @Transaction
    @Query("SELECT * FROM recurringseriesentity WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun observeSeriesByGroupId(groupId: String): Flow<List<RecurringSeriesWithDetails>>

    @Transaction
    @Query("SELECT * FROM recurringseriesentity WHERE seriesId = :seriesId")
    fun observeSeriesById(seriesId: String): Flow<RecurringSeriesWithDetails?>

    @Upsert
    suspend fun upsertSeries(series: List<RecurringSeriesEntity>)

    @Upsert
    suspend fun upsertSplits(splits: List<RecurringTemplateSplitEntity>)

    @Upsert
    suspend fun upsertExceptions(exceptions: List<RecurringExceptionEntity>)

    @Query("DELETE FROM recurringtemplatesplitentity WHERE seriesId IN (:seriesIds)")
    suspend fun deleteSplitsBySeriesIds(seriesIds: List<String>)

    @Query("DELETE FROM recurringexceptionentity WHERE seriesId IN (:seriesIds)")
    suspend fun deleteExceptionsBySeriesIds(seriesIds: List<String>)

    @Query("DELETE FROM recurringseriesentity WHERE seriesId IN (:seriesIds)")
    suspend fun deleteSeriesByIds(seriesIds: List<String>)

    @Query("SELECT seriesId FROM recurringseriesentity")
    suspend fun getAllSeriesIds(): List<String>

    @Query("SELECT seriesId FROM recurringseriesentity WHERE groupId = :groupId")
    suspend fun getSeriesIdsForGroup(groupId: String): List<String>

    @Query("SELECT groupId FROM recurringseriesentity WHERE seriesId = :seriesId")
    suspend fun getGroupIdForSeries(seriesId: String): String?

    @Query("UPDATE recurringseriesentity SET isActive = 0, updatedAt = :updatedAt WHERE seriesId = :seriesId")
    suspend fun markEnded(
        seriesId: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM recurringexceptionentity WHERE seriesId = :seriesId AND occurrenceDate = :occurrenceDate")
    suspend fun deleteException(
        seriesId: String,
        occurrenceDate: String,
    )

    /**
     * Replaces one series and its full set of splits and exceptions.
     *
     * Splits and exceptions are wiped before reinsert rather than upserted: a removed split or an
     * un-skipped date has no row in the payload to overwrite the stale one, so an upsert-only merge
     * would leave it behind and quietly change what the template means.
     */
    @Transaction
    suspend fun upsertSeriesWithDetails(
        series: RecurringSeriesEntity,
        splits: List<RecurringTemplateSplitEntity>,
        exceptions: List<RecurringExceptionEntity>,
    ) {
        upsertSeries(listOf(series))
        deleteSplitsBySeriesIds(listOf(series.seriesId))
        deleteExceptionsBySeriesIds(listOf(series.seriesId))
        if (splits.isNotEmpty()) upsertSplits(splits)
        if (exceptions.isNotEmpty()) upsertExceptions(exceptions)
    }

    /**
     * Applies a batch of series from `/api/sync` or a per-group refresh.
     *
     * [staleSeriesIds] are ids to prune — the complete local set minus the payload on a full sync or
     * a group refresh, and empty on a delta, where the payload only carries what changed. A series
     * is never deleted server-side, only deactivated, so a delta legitimately says nothing about
     * the ones it omits.
     */
    @Transaction
    suspend fun applySyncedSeries(
        series: List<RecurringSeriesEntity>,
        splitsBySeriesId: Map<String, List<RecurringTemplateSplitEntity>>,
        exceptionsBySeriesId: Map<String, List<RecurringExceptionEntity>>,
        staleSeriesIds: List<String>,
    ) {
        if (staleSeriesIds.isNotEmpty()) {
            deleteSplitsBySeriesIds(staleSeriesIds)
            deleteExceptionsBySeriesIds(staleSeriesIds)
            deleteSeriesByIds(staleSeriesIds)
        }
        if (series.isEmpty()) return

        upsertSeries(series)
        val seriesIds = series.map { it.seriesId }
        deleteSplitsBySeriesIds(seriesIds)
        deleteExceptionsBySeriesIds(seriesIds)

        val splits = seriesIds.flatMap { splitsBySeriesId[it].orEmpty() }
        if (splits.isNotEmpty()) upsertSplits(splits)

        val exceptions = seriesIds.flatMap { exceptionsBySeriesId[it].orEmpty() }
        if (exceptions.isNotEmpty()) upsertExceptions(exceptions)
    }
}
