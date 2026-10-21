package de.tabmates.features.tabgroup.presentation.util

import de.tabmates.features.tabgroup.domain.models.GroupParticipant

/**
 * Display order for people lists: the current user first, then A–Z by name.
 *
 * Common code has no locale collator, so names are compared case-insensitively with common Latin
 * accents folded onto their base letter (Ärger sorts with A, not after Z). The raw name and then the
 * id break ties, so the order is stable.
 */
fun List<GroupParticipant>.sortedForDisplay(currentUserId: String?): List<GroupParticipant> =
    sortedWith(
        compareByDescending<GroupParticipant> { currentUserId != null && it.userId == currentUserId }
            .thenBy { it.username.nameSortKey() }
            .thenBy { it.username }
            .thenBy { it.userId },
    )

internal fun String.nameSortKey(): String =
    buildString {
        this@nameSortKey.lowercase().forEach { char -> append(FOLDED[char] ?: char) }
    }

private val FOLDED: Map<Char, String> =
    buildMap {
        fun fold(
            chars: String,
            base: String,
        ) = chars.forEach { put(it, base) }
        fold("àáâãäåā", "a")
        fold("çčć", "c")
        fold("èéêëēė", "e")
        fold("ìíîïī", "i")
        fold("ñń", "n")
        fold("òóôõöøō", "o")
        fold("šś", "s")
        fold("ùúûüū", "u")
        fold("ýÿ", "y")
        fold("žźż", "z")
        put('ß', "ss")
        put('æ', "ae")
        put('œ', "oe")
    }
