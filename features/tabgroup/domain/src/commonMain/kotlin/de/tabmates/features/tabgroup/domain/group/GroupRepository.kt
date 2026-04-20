package de.tabmates.features.tabgroup.domain.group

import de.tabmates.features.tabgroup.domain.models.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroups(): Flow<List<Group>>
}
