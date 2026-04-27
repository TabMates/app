package de.tabmates.features.tabgroup.domain.group

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroups(): Flow<List<Group>>

    suspend fun fetchGroups(): Result<List<Group>, DataError.Remote>
}
