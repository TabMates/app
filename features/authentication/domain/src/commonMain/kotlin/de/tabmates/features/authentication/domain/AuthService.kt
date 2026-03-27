package de.tabmates.features.authentication.domain

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult

interface AuthService {
    suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote>
}
