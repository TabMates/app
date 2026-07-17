package de.tabmates.features.tabgroup.domain.models

enum class ParticipantType {
    REGISTERED,
    ANONYMOUS,
    PLACEHOLDER,
    DELETED,
}

data class GroupParticipant(
    val userId: String,
    val username: String,
    val participantType: ParticipantType,
) {
    val initials: String
        get() = username.take(2).uppercase()
}
