package de.tabmates.features.tabgroup.presentation.navigation.joingroup

import de.tabmates.core.presentation.util.UiText
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview

sealed interface JoinOption {
    data object NewMember : JoinOption

    data class ClaimPlaceholder(
        val placeholderId: String,
        val placeholderName: String,
    ) : JoinOption
}

sealed interface JoinGroupState {
    data object Loading : JoinGroupState

    data class Ready(
        val preview: GroupInvitePreview?,
        val token: String,
        val selectedOption: JoinOption,
        val isJoining: Boolean = false,
    ) : JoinGroupState

    data object Expired : JoinGroupState
}

sealed interface JoinGroupAction {
    data class SelectOption(val option: JoinOption) : JoinGroupAction

    data object Submit : JoinGroupAction
}

sealed interface JoinGroupEvent {
    data class Joined(val groupId: String) : JoinGroupEvent

    data class Error(val message: UiText) : JoinGroupEvent
}
