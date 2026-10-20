package de.tabmates.features.tabgroup.presentation.util

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * The JDK's built-in collation rules (unlike ICU on Android) keep some accented letters apart from
 * their base letter, e.g. Ё sorts before А. Stripping the accents first puts them back in place.
 * Only the generic accent block goes: marks such as the Devanagari virama carry meaning for sorting.
 */
actual fun platformNameComparator(): Comparator<String> {
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    return compareBy(collator) { it.withoutCombiningMarks() }
}

private val COMBINING_MARKS = Regex("\\p{InCombiningDiacriticalMarks}+")

private fun String.withoutCombiningMarks(): String =
    COMBINING_MARKS.replace(Normalizer.normalize(this, Normalizer.Form.NFD), "")
