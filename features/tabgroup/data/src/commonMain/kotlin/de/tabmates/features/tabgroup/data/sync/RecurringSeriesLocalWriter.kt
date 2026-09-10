package de.tabmates.features.tabgroup.data.sync

import de.tabmates.features.tabgroup.data.dto.RecurringSeriesDto
import de.tabmates.features.tabgroup.data.mappers.referencedParticipants
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.mappers.toExceptionEntities
import de.tabmates.features.tabgroup.data.mappers.toSplitEntities
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.types.ParticipantTypeDatabase
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import org.koin.core.annotation.Single

/**
 * The single place recurring schedules are written to the local mirror.
 *
 * Shared by the sync path, the per-group refresh and every schedule write, because all three have
 * the same foreign-key problem to solve first: a series' creator, payer, receiver and split
 * participants each need a `group_participants` row to exist, and a template routinely outlives the
 * membership of the people in it. Leaving that to each caller is how one of them ends up being the
 * one that crashes a sync on a constraint.
 */
@Single
class RecurringSeriesLocalWriter(
    private val database: TabMatesDatabase,
) {
    /**
     * Persists [series] and prunes [staleSeriesIds].
     *
     * [namedParticipants] are the participants the payload described in full; anyone a series
     * references without one gets an insert-ignore placeholder so the foreign keys hold.
     *
     * Pass stale ids only when the payload is complete — a full sync, or a per-group refresh. A
     * delta carries only what changed, and a series is never deleted server-side (only deactivated),
     * so pruning what a delta omits would wipe every schedule that simply had a quiet week.
     */
    suspend fun persist(
        series: List<RecurringSeries>,
        namedParticipants: List<GroupParticipant> = emptyList(),
        staleSeriesIds: List<String> = emptyList(),
    ) {
        if (series.isEmpty() && staleSeriesIds.isEmpty()) return

        ensureParticipantsExist(series, namedParticipants)

        database.recurringSeriesDao.applySyncedSeries(
            series = series.map { it.toEntity() },
            splitsBySeriesId = series.associate { it.seriesId to it.toSplitEntities() },
            exceptionsBySeriesId = series.associate { it.seriesId to it.toExceptionEntities() },
            staleSeriesIds = staleSeriesIds,
        )
    }

    /** Convenience for the write paths, which hold the server's own response. */
    suspend fun persist(
        series: List<RecurringSeriesDto>,
        staleSeriesIds: List<String> = emptyList(),
    ) = persist(
        series = series.map { it.toDomain() },
        namedParticipants =
            series
                .flatMap { it.referencedParticipants() }
                .distinctBy { it.userId }
                .map { it.toDomain() },
        staleSeriesIds = staleSeriesIds,
    )

    /**
     * Makes every participant a series references resolvable before the series is written.
     *
     * Named participants are upserted with what the payload knows. Everyone else a series names —
     * split participants are the only ones the server may leave unnamed — gets an insert-ignore
     * placeholder, which never overwrites a real row but does keep the foreign keys satisfiable.
     * Same last-resort guard the entry sync path uses, for the same reason.
     */
    private suspend fun ensureParticipantsExist(
        series: List<RecurringSeries>,
        namedParticipants: List<GroupParticipant>,
    ) {
        val named = namedParticipants.distinctBy { it.userId }
        if (named.isNotEmpty()) {
            database.groupParticipantDao.upsertParticipants(named.map { it.toEntity() })
        }

        val namedIds = named.mapTo(mutableSetOf()) { it.userId }
        val unresolved =
            series
                .flatMap { candidate ->
                    buildList {
                        add(candidate.createdBy.userId)
                        add(candidate.rule.paidByUserId)
                        candidate.rule.receivedByUserId?.let(::add)
                        candidate.rule.splits.mapTo(this) { it.participantId }
                    }
                }.distinct()
                .filterNot { it in namedIds }
        if (unresolved.isNotEmpty()) {
            database.groupParticipantDao.insertParticipantsIgnoringConflicts(
                unresolved.map {
                    GroupParticipantEntity(
                        userId = it,
                        username = "Unknown",
                        participantType = ParticipantTypeDatabase.PLACEHOLDER,
                    )
                },
            )
        }
    }
}
