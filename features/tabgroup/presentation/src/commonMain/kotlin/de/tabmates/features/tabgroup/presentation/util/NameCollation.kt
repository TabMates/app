package de.tabmates.features.tabgroup.presentation.util

/**
 * Compares names the way the device locale sorts them, ignoring case and accents (Ärger sorts
 * with A, Ёлка with Е). Backed by the platform collator, so non-Latin scripts sort correctly too.
 */
expect fun platformNameComparator(): Comparator<String>
