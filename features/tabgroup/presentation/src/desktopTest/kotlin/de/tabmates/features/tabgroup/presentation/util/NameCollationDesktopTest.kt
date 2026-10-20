package de.tabmates.features.tabgroup.presentation.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NameCollationDesktopTest {
    @Test
    fun cyrillicYoSortsWithYe() {
        val sorted = listOf("Яна", "Ёлка", "Борис").sortedWith(platformNameComparator())

        assertEquals(listOf("Борис", "Ёлка", "Яна"), sorted)
    }

    @Test
    fun viramaIsNotStrippedAway() {
        // Without the virama, "क्" would collapse to "क" and jump ahead of "कख".
        val sorted = listOf("क्", "कख").sortedWith(platformNameComparator())

        assertEquals(listOf("कख", "क्"), sorted)
    }
}
