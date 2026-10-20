package de.tabmates.features.tabgroup.presentation.util

import java.text.Collator
import java.util.Locale

actual fun platformNameComparator(): Comparator<String> {
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    return Comparator { a, b -> collator.compare(a, b) }
}
