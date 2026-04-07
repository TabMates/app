package de.tabmates.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import de.tabmates.core.domain.util.Loggable
import kotlinx.serialization.Serializable

@Serializable
abstract class LoggableNavKey : Loggable(), NavKey
