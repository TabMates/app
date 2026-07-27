package de.tabmates.features.tabgroup.data.tabentry

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What a queued delete remembers about the entry it removes.
 *
 * Deleting wipes the local row immediately (see `OfflineFirstTabEntryRepository.deleteTabEntry`), so
 * by the time the activity feed renders the pending row there is no source left for its title or
 * amount — the same reason the server snapshots them into its own `ENTRY_DELETED` event.
 *
 * Every field but [tabEntryId] is nullable so a row written by an older build, whose payload was the
 * bare entry id, still dispatches (see `TabEntryOutbox.dispatchDelete`); such a row simply renders
 * without a title until it drains.
 */
@Serializable
internal data class PendingDeletePayload(
    val tabEntryId: String,
    val groupId: String? = null,
    val title: String? = null,
    val amount: Double? = null,
    val currencyCode: String? = null,
    /** Name of a `TabEntryTypeDatabase` constant; unparseable values render as a generic entry. */
    val entryType: String? = null,
)

/**
 * Reads a queued delete's payload in either shape: the JSON object written by this build, or the bare
 * entry id written by builds before the snapshot existed. A user upgrading mid-flight can have both
 * sitting in their outbox at once.
 */
internal fun Json.decodePendingDeletePayload(payload: String): PendingDeletePayload =
    if (payload.trimStart().startsWith('{')) {
        decodeFromString(PendingDeletePayload.serializer(), payload)
    } else {
        PendingDeletePayload(tabEntryId = payload)
    }
