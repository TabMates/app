package de.tabmates.features.tabgroup.presentation.navigation.addexpense

/**
 * The kind of split-carrying tab entry the add/edit + detail screens operate on. Picked via the
 * on-screen toggle in create mode and fixed to the loaded entry's kind in edit mode. Settlements
 * are handled by their own dedicated screens and are not represented here.
 */
enum class EntryKind {
    EXPENSE,
    INCOME,
}
