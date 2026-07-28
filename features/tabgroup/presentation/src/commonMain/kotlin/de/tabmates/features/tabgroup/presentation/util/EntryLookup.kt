package de.tabmates.features.tabgroup.presentation.util

import de.tabmates.features.tabgroup.domain.models.TabEntry

/**
 * The result of looking an entry up by id, for the detail screens.
 *
 * The repository hands back a `Flow<TabEntry?>` backed by a Room query, which emits `null` for a row
 * that isn't there and then stays subscribed forever. Detail screens seed that flow with a `null` of
 * their own so [combine][kotlinx.coroutines.flow.combine] can produce a first state, which leaves
 * the two cases indistinguishable — and a screen that shows a spinner until an entry arrives spins
 * forever when the entry was deleted. Wrapping the emissions separates them.
 */
internal sealed interface EntryLookup {
    /** The query has not answered yet. */
    data object Loading : EntryLookup

    /** The query answered; [entry] is null when no such row exists. */
    data class Loaded(val entry: TabEntry?) : EntryLookup
}
