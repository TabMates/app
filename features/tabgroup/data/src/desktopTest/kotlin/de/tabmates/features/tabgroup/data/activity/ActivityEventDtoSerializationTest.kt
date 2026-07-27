package de.tabmates.features.tabgroup.data.activity

import de.tabmates.features.tabgroup.data.dto.ActivityFeedResponseDto
import de.tabmates.features.tabgroup.domain.activity.ActivityEntryType
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityField
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ActivityEventDtoSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun knownConstantsDecodeByName() {
        val page = json.decodeFromString(ActivityFeedResponseDto.serializer(), pageJson("ENTRY_UPDATED", "AMOUNT"))

        val event = page.events.single()
        assertEquals(ActivityEventType.ENTRY_UPDATED, event.type)
        assertEquals(ActivityEntryType.EXPENSE, event.entryType)
        assertEquals(ActivityField.AMOUNT, event.changes.single().field)
    }

    /**
     * The page is decoded in one call, so without the fallback a single unrecognised constant throws
     * and takes the whole page — and with it the sync cursor — down. The row has to survive as
     * UNKNOWN and still render.
     */
    @Test
    fun anUnrecognisedEventTypeDecodesAsUnknownInsteadOfThrowing() {
        val page =
            json.decodeFromString(
                ActivityFeedResponseDto.serializer(),
                pageJson("ENTRY_ARCHIVED", "AMOUNT"),
            )

        val event = page.events.single()
        assertEquals(ActivityEventType.UNKNOWN, event.type)
        // Everything else on the row still decodes, so the feed can render an actor line.
        assertEquals("Dinner", event.entryTitle)
        assertEquals(7L, event.seq)
    }

    @Test
    fun anUnrecognisedChangeFieldDecodesAsUnknown() {
        val page =
            json.decodeFromString(
                ActivityFeedResponseDto.serializer(),
                pageJson("ENTRY_UPDATED", "TAX_RATE"),
            )

        assertEquals(
            ActivityField.UNKNOWN,
            page.events
                .single()
                .changes
                .single()
                .field,
        )
    }

    @Test
    fun anUnrecognisedEntryTypeDecodesAsUnknown() {
        val page =
            json.decodeFromString(
                ActivityFeedResponseDto.serializer(),
                pageJson("ENTRY_UPDATED", "AMOUNT", entryType = "REFUND"),
            )

        assertEquals(ActivityEntryType.UNKNOWN, page.events.single().entryType)
    }

    private fun pageJson(
        type: String,
        field: String,
        entryType: String = "EXPENSE",
    ): String =
        """
        {
          "events": [
            {
              "id": "a1",
              "seq": 7,
              "groupId": "g1",
              "occurredAt": "2026-07-27T10:00:00Z",
              "actorUserId": "u1",
              "type": "$type",
              "tabEntryId": "e1",
              "entryType": "$entryType",
              "entryTitle": "Dinner",
              "amount": 12.5,
              "currencyCode": "EUR",
              "entryVersion": 1,
              "changes": [{ "field": "$field", "oldValue": "10.0", "newValue": "12.5" }]
            }
          ],
          "nextCursor": 7,
          "hasMore": false
        }
        """.trimIndent()
}
