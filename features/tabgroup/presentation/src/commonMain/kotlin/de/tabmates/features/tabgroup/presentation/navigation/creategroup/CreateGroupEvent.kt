package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.core.presentation.util.UiText

sealed interface CreateGroupEvent {
    data object GroupCreated : CreateGroupEvent

    data object InviteSent : CreateGroupEvent

    data object LinkCopied : CreateGroupEvent

    data object LinkShared : CreateGroupEvent

    data class Error(val message: UiText) : CreateGroupEvent
}
