package de.tabmates.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserWithPendingEmailSerializable(
    val user: UserSerializable,
    val pendingEmail: String? = null,
)
