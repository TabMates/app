package de.tabmates.features.tabgroup.presentation.util

import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A group together with every participant name the client knows, member or not. */
internal data class GroupWithParticipants(
    val group: Group? = null,
    val participantsById: Map<String, GroupParticipant> = emptyMap(),
)

/**
 * Observes [groupId] alongside a name lookup that survives removal.
 *
 * [GroupRepository.getGroups] carries active members only, so resolving a payer or a split through
 * `group.participants` renders anyone who has since been removed as an anonymous placeholder — even
 * though their expenses are untouched and the client still holds their username. Overlaying the
 * global participant table fixes that; the group's own copy wins where both know a person, since it
 * is the one that follows a rename.
 *
 * Also keeps callers within the typed `combine` overloads by pre-combining the two flows. It emits
 * nothing until both queries have answered, so a caller that needs `combine` to produce a first
 * state before then seeds it with `onStart { emit(GroupWithParticipants()) }`.
 */
internal fun GroupRepository.observeGroupWithParticipants(groupId: String): Flow<GroupWithParticipants> =
    combine(getGroups(), getAllParticipants()) { groups, allParticipants ->
        val group = groups.firstOrNull { it.id == groupId }
        GroupWithParticipants(
            group = group,
            participantsById =
                (allParticipants + group?.participants.orEmpty()).associateBy { it.userId },
        )
    }
