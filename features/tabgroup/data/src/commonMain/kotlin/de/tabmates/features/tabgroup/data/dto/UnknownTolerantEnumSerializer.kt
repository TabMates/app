package de.tabmates.features.tabgroup.data.dto

import de.tabmates.features.tabgroup.domain.activity.ActivityEntryType
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityField
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Reads an enum by name, falling back to [unknown] instead of throwing.
 *
 * kotlinx.serialization rejects an unrecognised enum value outright, and a page is decoded in one
 * call — so the day the server adds a constant this build has never heard of, the whole page throws,
 * `sync()` fails, and the cursor never advances past it. That page would stay poison until the app
 * updates. Landing on `UNKNOWN` keeps the row: it mirrors, and renders as a plain actor line.
 *
 * Scoped to the properties that need it rather than turning on `coerceInputValues`, which is set on
 * the shared `Json` and would also start silently coercing explicit nulls to defaults in every other
 * DTO in the app.
 */
abstract class UnknownTolerantEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val entries: List<T>,
    private val unknown: T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeString()
        return entries.firstOrNull { it.name == raw } ?: unknown
    }
}

object ActivityEventTypeSerializer : UnknownTolerantEnumSerializer<ActivityEventType>(
    serialName = "ActivityEventType",
    entries = ActivityEventType.entries,
    unknown = ActivityEventType.UNKNOWN,
)

object ActivityFieldSerializer : UnknownTolerantEnumSerializer<ActivityField>(
    serialName = "ActivityField",
    entries = ActivityField.entries,
    unknown = ActivityField.UNKNOWN,
)

object ActivityEntryTypeSerializer : UnknownTolerantEnumSerializer<ActivityEntryType>(
    serialName = "ActivityEntryType",
    entries = ActivityEntryType.entries,
    unknown = ActivityEntryType.UNKNOWN,
)
