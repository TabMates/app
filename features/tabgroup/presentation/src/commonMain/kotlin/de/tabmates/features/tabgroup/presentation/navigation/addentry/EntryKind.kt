package de.tabmates.features.tabgroup.presentation.navigation.addentry

/**
 * The kind of tab entry the add/edit + detail screens operate on.
 *
 * Picked via the on-screen toggle while creating and fixed to the loaded entry's kind while editing,
 * because an entry's type is not something the server lets change — and a recurring series' type is
 * fixed for its whole life.
 */
enum class EntryKind {
    EXPENSE,
    INCOME,

    /**
     * A payment from one member to another. Carries a receiver instead of splits, which is why the
     * form hides the split editor and shows a second person picker for this kind.
     */
    SETTLEMENT,
}
