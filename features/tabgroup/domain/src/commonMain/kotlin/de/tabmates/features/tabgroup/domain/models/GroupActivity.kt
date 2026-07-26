package de.tabmates.features.tabgroup.domain.models

import kotlin.time.Instant

/**
 * Wall-clock time of the last change in a group: the newer of [groupActivityAt] (the server-sourced
 * [Group.lastActivityAt], bumped on metadata changes) and the newest [TabEntry.lastModifiedAt] in
 * [entries]. Drives group list ordering, most recently changed first.
 *
 * Nothing bumps [Group.lastActivityAt] locally, so folding the entries in is what makes a group
 * with a freshly added or edited expense rise to the top before — or without — a server round trip.
 * Deleted entries count too: the server bumps [TabEntry.lastModifiedAt] when it soft-deletes, and a
 * local delete removes the row outright.
 */
fun latestActivityAt(
    groupActivityAt: Instant,
    entries: List<TabEntry>,
): Instant = entries.fold(groupActivityAt) { latest, entry -> maxOf(latest, entry.lastModifiedAt) }
