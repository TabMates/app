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
    /** Non-null while the confirm dialog for that person is open. */
    val removeTarget: RemoveTarget? = null,
    val isRemoving: Boolean = false,
)

data class GroupPerson(
    val id: String,
    val name: String,
    // Independent of [badge] on purpose: the group creator viewing their own row reads "You" and
    // still keeps the owner badge, which is how the old member row behaved.
    val isCurrentUser: Boolean = false,
    val badge: PersonBadge? = null,
    /**
     * False for yourself — leaving is a separate action in the group settings — and for the group's
     * creator, who the server refuses to remove. True for everyone else, members and placeholders
     * alike: membership is the only permission.
     */
    val canRemove: Boolean = false,
) {
    val initials: String get() = name.take(2).uppercase()
}

enum class PersonBadge { OWNER, PENDING }

/**
 * The person a confirm dialog is asking about.
 *
 * Carries raw pieces rather than a finished sentence: composing it needs `getString`, which must
 * not be called from a ViewModel.
 */
data class RemoveTarget(
    val id: String,
    val name: String,
    val isPlaceholder: Boolean,
    /** Their outstanding balance, pre-formatted, or null when they are settled up. */
    val outstanding: String? = null,
)

sealed interface GroupPeopleAction {
    data object AddPlaceholderClick : GroupPeopleAction

    /** Commits the typed name; the field stays open so the next one can follow straight away. */
    data object SubmitName : GroupPeopleAction

    data object CancelAdd : GroupPeopleAction

    data object RotateInvite : GroupPeopleAction

    data class RemoveClick(val personId: String) : GroupPeopleAction

    data object ConfirmRemove : GroupPeopleAction

    data object DismissRemove : GroupPeopleAction
}

sealed interface GroupPeopleEvent {
    data class Error(val message: UiText) : GroupPeopleEvent
}
