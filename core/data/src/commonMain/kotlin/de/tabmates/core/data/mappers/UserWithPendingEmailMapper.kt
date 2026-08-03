package de.tabmates.core.data.mappers

import de.tabmates.core.data.dto.UserWithPendingEmailSerializable
import de.tabmates.core.domain.auth.UserWithPendingEmail

fun UserWithPendingEmailSerializable.toDomain(): UserWithPendingEmail {
    return UserWithPendingEmail(
        user = user.toDomain(),
        pendingEmail = pendingEmail,
    )
}
