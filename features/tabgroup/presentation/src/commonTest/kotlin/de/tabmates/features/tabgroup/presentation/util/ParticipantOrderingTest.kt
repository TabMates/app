package de.tabmates.features.tabgroup.presentation.util

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantOrderingTest {
    private fun person(
        id: String,
        name: String,
    ) = GroupParticipant(userId = id, username = name, participantType = ParticipantType.REGISTERED)

    @Test
    fun currentUserComesFirstThenAlphabetical() {
        val people = listOf(person("c", "Carl"), person("me", "Zoe"), person("a", "anna"), person("b", "Ben"))

        val sorted = people.sortedForDisplay(currentUserId = "me")

        assertEquals(listOf("Zoe", "anna", "Ben", "Carl"), sorted.map { it.username })
    }

    @Test
    fun accentsSortWithTheirBaseLetter() {
        val people = listOf(person("1", "Zora"), person("2", "Ben"), person("3", "Ärger"), person("4", "Anna"))

        val sorted = people.sortedForDisplay(currentUserId = null)

        assertEquals(listOf("Anna", "Ärger", "Ben", "Zora"), sorted.map { it.username })
    }

    @Test
    fun sameNamesKeepAStableOrder() {
        val people = listOf(person("2", "Max"), person("1", "max"), person("3", "Max"))

        val sorted = people.sortedForDisplay(currentUserId = null)

        assertEquals(listOf("2", "3", "1"), sorted.map { it.userId })
    }

    @Test
    fun missingCurrentUserLeavesPlainAlphabeticalOrder() {
        val people = listOf(person("b", "Ben"), person("a", "Anna"))

        val sorted = people.sortedForDisplay(currentUserId = "someone-else")

        assertEquals(listOf("Anna", "Ben"), sorted.map { it.username })
    }
}
