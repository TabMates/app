package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.core.presentation.util.UiText

sealed interface CreateGroupEvent {
    data object GroupCreated : CreateGroupEvent

    data class Error(val message: UiText) : CreateGroupEvent
}
