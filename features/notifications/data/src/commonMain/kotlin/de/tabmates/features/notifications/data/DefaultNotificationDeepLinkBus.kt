package de.tabmates.features.notifications.data

import de.tabmates.features.notifications.domain.NotificationDeepLinkBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single

@Single(binds = [NotificationDeepLinkBus::class])
class DefaultNotificationDeepLinkBus : NotificationDeepLinkBus {
    // Buffered so the non-suspending publish() from notification callbacks never drops or blocks.
    private val _deepLinks = MutableSharedFlow<String>(extraBufferCapacity = 16)

    override val deepLinks: Flow<String> = _deepLinks.asSharedFlow()

    override fun publish(uri: String) {
        _deepLinks.tryEmit(uri)
    }
}
