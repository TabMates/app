package de.tabmates.core.data.environment

import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.util.EmptyResult

/**
 * Asks a candidate backend whether it is really there and really accepts these credentials, before
 * the app commits to it and throws away the local data of the environment it is leaving.
 */
interface EnvironmentProbe {
    suspend fun probe(
        httpBaseUrl: String,
        apiKey: String,
    ): EmptyResult<EnvironmentSwitchError>
}
