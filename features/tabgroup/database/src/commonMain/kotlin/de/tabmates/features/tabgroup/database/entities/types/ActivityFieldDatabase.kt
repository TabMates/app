package de.tabmates.features.tabgroup.database.entities.types

/**
 * The field an activity change row reports a before/after value for. Names match the server's
 * `ActivityChangeField`.
 *
 * [SPLITS] is a flag, not a diff: both values are null and it only says "the split distribution
 * changed". [PAID_BY] / [RECEIVED_BY] hold raw user ids, resolved to names at render time so a later
 * rename cannot make an old row lie.
 */
enum class ActivityFieldDatabase {
    TITLE,
    DESCRIPTION,
    AMOUNT,
    CURRENCY,
    EXCHANGE_RATE,
    ENTRY_DATE,
    PAID_BY,
    RECEIVED_BY,
    SPLITS,
    DEFAULT_CURRENCY,
    UNKNOWN,
}
