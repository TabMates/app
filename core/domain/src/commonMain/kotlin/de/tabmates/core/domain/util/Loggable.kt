package de.tabmates.core.domain.util

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
abstract class Loggable {
    @OptIn(InternalSerializationApi::class)
    override fun toString(): String = this::class.serializer().descriptor.serialName
}
