package de.tabmates.features.tabgroup.data.mappers

import de.tabmates.features.tabgroup.data.dto.ActivityChangeDto
import de.tabmates.features.tabgroup.data.dto.ActivityEventDto
import de.tabmates.features.tabgroup.database.entities.ActivityEventEntity
import de.tabmates.features.tabgroup.database.entities.ActivityEventWithChanges
import de.tabmates.features.tabgroup.database.entities.ActivityFieldChangeEntity
import de.tabmates.features.tabgroup.database.entities.types.ActivityEventTypeDatabase
import de.tabmates.features.tabgroup.database.entities.types.ActivityFieldDatabase
import de.tabmates.features.tabgroup.database.entities.types.TabEntryTypeDatabase
import de.tabmates.features.tabgroup.domain.activity.ActivityEntryType
import de.tabmates.features.tabgroup.domain.activity.ActivityEvent
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityField
import de.tabmates.features.tabgroup.domain.activity.ActivityFieldChange
import kotlin.time.Instant

fun ActivityEventDto.toDomain(): ActivityEvent =
    ActivityEvent(
        id = id,
        seq = seq,
        groupId = groupId,
        occurredAt = occurredAt,
        actorUserId = actorUserId,
        type = type,
        tabEntryId = tabEntryId,
        entryType = entryType,
        entryTitle = entryTitle,
        amount = amount,
        currencyCode = currencyCode,
        targetUserId = targetUserId,
        targetUsername = targetUsername,
        entryVersion = entryVersion,
        changes = changes.map { it.toDomain() },
    )

fun ActivityChangeDto.toDomain(): ActivityFieldChange =
    ActivityFieldChange(
        field = field,
        oldValue = oldValue,
        newValue = newValue,
    )

fun ActivityEvent.toEntity(): ActivityEventEntity =
    ActivityEventEntity(
        id = id,
        seq = seq,
        groupId = groupId,
        occurredAt = occurredAt.toEpochMilliseconds(),
        actorUserId = actorUserId,
        type = type.toDatabase(),
        tabEntryId = tabEntryId,
        entryType = entryType?.toDatabase(),
        entryTitle = entryTitle,
        amount = amount,
        currencyCode = currencyCode,
        targetUserId = targetUserId,
        targetUsername = targetUsername,
        entryVersion = entryVersion,
    )

/**
 * The children of one event. Their keys are generated, so they are always written as a set —
 * `ActivityEventDao.upsertPage` clears an event's rows before reinserting them.
 */
fun ActivityEvent.toChangeEntities(): List<ActivityFieldChangeEntity> =
    changes.map { change ->
        ActivityFieldChangeEntity(
            activityEventId = id,
            field = change.field.toDatabase(),
            oldValue = change.oldValue,
            newValue = change.newValue,
        )
    }

fun ActivityEventWithChanges.toDomain(): ActivityEvent =
    ActivityEvent(
        id = event.id,
        seq = event.seq,
        groupId = event.groupId,
        occurredAt = Instant.fromEpochMilliseconds(event.occurredAt),
        actorUserId = event.actorUserId,
        type = event.type.toDomain(),
        tabEntryId = event.tabEntryId,
        entryType = event.entryType?.toDomain(),
        entryTitle = event.entryTitle,
        amount = event.amount,
        currencyCode = event.currencyCode,
        targetUserId = event.targetUserId,
        targetUsername = event.targetUsername,
        entryVersion = event.entryVersion,
        changes =
            changes.map { change ->
                ActivityFieldChange(
                    field = change.field.toDomain(),
                    oldValue = change.oldValue,
                    newValue = change.newValue,
                )
            },
    )

/**
 * Widens the entry type held in a queued delete's snapshot payload, which is a plain string rather
 * than a serialized enum. The wire DTOs need no equivalent — their serializers already fall back.
 */
fun String.toActivityEntryType(): ActivityEntryType =
    ActivityEntryType.entries.firstOrNull { it.name == this } ?: ActivityEntryType.UNKNOWN

private fun ActivityEventType.toDatabase(): ActivityEventTypeDatabase =
    ActivityEventTypeDatabase.entries.firstOrNull { it.name == name } ?: ActivityEventTypeDatabase.UNKNOWN

private fun ActivityEventTypeDatabase.toDomain(): ActivityEventType =
    ActivityEventType.entries.firstOrNull { it.name == name } ?: ActivityEventType.UNKNOWN

private fun ActivityField.toDatabase(): ActivityFieldDatabase =
    ActivityFieldDatabase.entries.firstOrNull { it.name == name } ?: ActivityFieldDatabase.UNKNOWN

private fun ActivityFieldDatabase.toDomain(): ActivityField =
    ActivityField.entries.firstOrNull { it.name == name } ?: ActivityField.UNKNOWN

/**
 * `TabEntryTypeDatabase` has no `UNKNOWN`, so an entry type this build does not know is dropped to
 * null rather than mapped — the row still renders, just without a type-specific icon.
 */
private fun ActivityEntryType.toDatabase(): TabEntryTypeDatabase? =
    TabEntryTypeDatabase.entries.firstOrNull {
        it.name ==
            name
    }

private fun TabEntryTypeDatabase.toDomain(): ActivityEntryType =
    ActivityEntryType.entries.firstOrNull { it.name == name } ?: ActivityEntryType.UNKNOWN
