package de.tabmates.features.tabgroup.domain.recurring

/**
 * How often a recurring series repeats.
 *
 * Deliberately a small closed set rather than an RFC 5545 `RRULE`: combined with
 * [RecurringRule.interval] it covers rent, subscriptions and salaries without a calendar library.
 * Mirrors the server enum of the same name — the wire carries these names verbatim.
 */
enum class RecurrenceFrequency {
    DAILY,

    /**
     * Repeats on the same weekday as the rule's start date. There is no multi-weekday set: a
     * schedule falling on both Monday and Thursday is two series.
     */
    WEEKLY,

    /**
     * Repeats on the same day of the month as the rule's start date, clamped to the last day of
     * shorter months — a series anchored on the 31st falls on the 30th in April and the 28th or
     * 29th in February, then returns to the 31st.
     */
    MONTHLY,

    /**
     * Repeats on the same month and day as the rule's start date. A series anchored on 29 February
     * falls on the 28th in non-leap years.
     */
    YEARLY,
}
