package de.tabmates.features.tabgroup.domain.models

/**
 * Every participant id these entries actually reference — payers, split participants and
 * settlement receivers.
 *
 * This is "who is in the money", as opposed to "who is currently a member": removing someone from a
 * group only drops their membership, so their expenses, splits and settlements stay exactly as they
 * are. Anything that resolves names or computes balances has to work from this set unioned with the
 * active members, or a former member's money silently disappears from the maths.
 */
fun List<TabEntry>.referencedParticipantIds(): Set<String> =
    buildSet {
        this@referencedParticipantIds.forEach { entry ->
            add(entry.paidByUserId)
            when (entry) {
                is TabEntry.Expense -> entry.splits.forEach { add(it.participantId) }
                is TabEntry.Income -> entry.splits.forEach { add(it.participantId) }
                is TabEntry.Settlement -> add(entry.receivedByUserId)
            }
        }
    }
