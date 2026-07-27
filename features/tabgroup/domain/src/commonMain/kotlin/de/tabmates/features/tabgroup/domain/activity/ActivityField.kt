package de.tabmates.features.tabgroup.domain.activity

/**
 * The field an [ActivityFieldChange] reports a before/after value for. Names match the server's
 * `ActivityChangeField`; [UNKNOWN] absorbs fields a future server adds.
 */
enum class ActivityField {
    TITLE,
    DESCRIPTION,
    AMOUNT,
    CURRENCY,
    EXCHANGE_RATE,
    ENTRY_DATE,

    /** Carries a raw user id, resolved to a name at render time. */
    PAID_BY,

    /** Carries a raw user id, resolved to a name at render time. */
    RECEIVED_BY,

    /** A flag, not a diff: both values are null and it only says the distribution changed. */
    SPLITS,
    DEFAULT_CURRENCY,
    UNKNOWN,
}
