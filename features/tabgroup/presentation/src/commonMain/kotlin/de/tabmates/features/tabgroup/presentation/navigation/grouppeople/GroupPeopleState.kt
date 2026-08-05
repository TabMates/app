package de.tabmates.features.tabgroup.presentation.navigation.grouppeople

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.core.presentation.util.UiText

data class GroupPeopleState(
    val isLoading: Boolean = true,
    val members: List<GroupPerson> = emptyList(),
    val placeholders: List<GroupPerson> = emptyList(),
    val inviteToken: String = "",
    /** True while the inline "new placeholder" field is open at the end of the list. */
    val isAddRowVisible: Boolean = false,
    val newNameTextState: TextFieldState = TextFieldState(),
    val isAddingPlaceholder: Boolean = false,
)

data class GroupPerson(
    val id: String,
    val name: String,
    // Independent of [badge] on purpose: the group creator viewing their own row reads "You" and
    // still keeps the owner badge, which is how the old member row behaved.
    val isCurrentUser: Boolean = false,
    val badge: PersonBadge? = null,
) {
    val initials: String get() = name.take(2).uppercase()
}

enum class PersonBadge { OWNER, PENDING }

sealed interface GroupPeopleAction {
    data object AddPlaceholderClick : GroupPeopleAction

    /** Commits the typed name; the field stays open so the next one can follow straight away. */
    data object SubmitName : GroupPeopleAction

    data object CancelAdd : GroupPeopleAction

    data object RotateInvite : GroupPeopleAction
}

sealed interface GroupPeopleEvent {
    data class Error(val message: UiText) : GroupPeopleEvent
}
