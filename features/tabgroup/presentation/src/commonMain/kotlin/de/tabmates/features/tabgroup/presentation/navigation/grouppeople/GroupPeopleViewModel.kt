package de.tabmates.features.tabgroup.presentation.navigation.grouppeople

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_error_duplicate_name
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class GroupPeopleViewModel(
    @InjectedParam private val groupId: String,
    private val groupRepository: GroupRepository,
    currentAccount: CurrentAccount,
) : ViewModel() {
    private val currentUserId = currentAccount.userId().orEmpty()

    /**
     * Held outside the state flow: a [TextFieldState] is a mutable holder the field edits in place,
     * so it has to survive every state rebuild.
     */
    private val newNameTextState = TextFieldState()

    private var isRotatingInvite = false

    /** Dialog-only state, kept apart so a group update never clobbers what is being typed. */
    private val form = MutableStateFlow(FormState())

    private val eventChannel = Channel<GroupPeopleEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    /**
     * The group is observed rather than read once: adding placeholders and rotating the invite both
     * write through the repository, and the list and the link then refresh on their own.
     */
    val state: StateFlow<GroupPeopleState> =
        combine(
            groupRepository
                .getGroups()
                .map { groups -> GroupLookup(hasLoaded = true, group = groups.firstOrNull { it.id == groupId }) }
                .onStart { emit(GroupLookup()) },
            form,
        ) { lookup, form ->
            val group = lookup.group
            GroupPeopleState(
                // Only the wait for the first emission is loading. A group that is genuinely absent
                // resolves to an empty screen instead of a spinner that never stops.
                isLoading = !lookup.hasLoaded,
                members = group?.members().orEmpty(),
                placeholders = group?.placeholders().orEmpty(),
                inviteToken = group?.inviteToken.orEmpty(),
                isAddRowVisible = form.isRowVisible,
                newNameTextState = newNameTextState,
                isAddingPlaceholder = form.isAdding,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = GroupPeopleState(),
        )

    fun onAction(action: GroupPeopleAction) {
        when (action) {
            GroupPeopleAction.AddPlaceholderClick -> {
                newNameTextState.clearText()
                form.update { it.copy(isRowVisible = true) }
            }

            GroupPeopleAction.SubmitName -> {
                submitName()
            }

            GroupPeopleAction.CancelAdd -> {
                newNameTextState.clearText()
                form.update { it.copy(isRowVisible = false) }
            }

            GroupPeopleAction.RotateInvite -> {
                rotateInvite()
            }
        }
    }

    /**
     * Adds the typed name on its own. One request per name rather than a batch, so the placeholder
     * shows up in the list the moment it exists — that appearing row is what tells the user the
     * still-open field takes another name.
     */
    private fun submitName() {
        if (form.value.isAdding) return
        val name = newNameTextState.text.toString().trim()
        if (name.isEmpty()) {
            form.update { it.copy(isRowVisible = false) }
            return
        }
        // Claimed before the coroutine starts, so a second tap cannot slip past the check above
        // while this one is still in flight.
        form.update { it.copy(isAdding = true) }
        viewModelScope.launch {
            try {
                if (isKnownName(name)) {
                    // Text stays put: the name is the thing that needs correcting.
                    eventChannel.send(
                        GroupPeopleEvent.Error(UiText.Resource(Res.string.group_people_error_duplicate_name)),
                    )
                    return@launch
                }
                groupRepository
                    .addNewParticipantsToGroup(groupId, listOf(name))
                    .onSuccess {
                        // Field stays open and empty, ready for the next name.
                        newNameTextState.clearText()
                    }.onFailure { error ->
                        // Keep the text so a retry does not mean typing it again.
                        eventChannel.send(GroupPeopleEvent.Error(error.toUiText()))
                    }
            } finally {
                form.update { it.copy(isAdding = false) }
            }
        }
    }

    /**
     * Read straight from the repository rather than from [state]: that flow is `WhileSubscribed`, so
     * with no collector it holds a stale or empty value and every name would look new.
     */
    private suspend fun isKnownName(name: String): Boolean {
        val group = groupRepository.getGroups().first().firstOrNull { it.id == groupId } ?: return false
        return group.participants.any { it.username.equals(name, ignoreCase = true) }
    }

    private fun rotateInvite() {
        if (isRotatingInvite) return
        isRotatingInvite = true
        viewModelScope.launch {
            try {
                groupRepository
                    .rotateInviteToken(groupId)
                    .onFailure { error -> eventChannel.send(GroupPeopleEvent.Error(error.toUiText())) }
            } finally {
                // In a finally block so a failure cannot leave rotation blocked for good.
                isRotatingInvite = false
            }
        }
    }

    private fun Group.members(): List<GroupPerson> =
        participants
            .filter { it.participantType != ParticipantType.PLACEHOLDER }
            .sortedWith(
                compareByDescending<GroupParticipant> { it.userId == currentUserId }.thenBy { it.username },
            ).map { participant ->
                GroupPerson(
                    id = participant.userId,
                    name = participant.username,
                    isCurrentUser = participant.userId == currentUserId,
                    badge = PersonBadge.OWNER.takeIf { participant.userId == creator.userId },
                )
            }

    private fun Group.placeholders(): List<GroupPerson> =
        participants
            .filter { it.participantType == ParticipantType.PLACEHOLDER }
            .sortedBy { it.username }
            .map { GroupPerson(id = it.userId, name = it.username, badge = PersonBadge.PENDING) }

    private data class FormState(
        val isRowVisible: Boolean = false,
        val isAdding: Boolean = false,
    )

    /** Separates "the group flow has not emitted yet" from "this group is not in the list". */
    private data class GroupLookup(
        val hasLoaded: Boolean = false,
        val group: Group? = null,
    )
}
