package de.tabmates.features.tabgroup.database.entities.types

/**
 * Which of a series' two mutually exclusive end columns is in use.
 *
 * The wire models this as a sealed type; the table keeps it a discriminator plus two nullable
 * columns so both stay queryable.
 */
enum class RecurringEndTypeDatabase {
    NEVER,
    UNTIL,
    COUNT,
}
