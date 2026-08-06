package de.tabmates.features.tabgroup.domain.group

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Someone else removed this user from [groupId]. [title] is the local name, if it was known. */
data class RemovedFromGroup(
    val groupId: String,
    val title: String?,
)

/**
 * Carries "you were removed" from the socket up to the app shell, which is the only place that can
 * pop the screens scoped to that group and say so.
 *
 * Modelled on [de.tabmates.core.domain.update.UpgradeRequiredNotifier], but an event rather than a
 * latch: being removed happens at a moment, and there is no state to keep afterwards — the group is
 * deleted locally either way. The buffer keeps [notifyRemoved] from ever suspending the collector
 * that reads the socket.
 *
 * Delivery is best-effort: with no replay, a removal arriving while nothing is subscribed is
 * dropped, and that costs the back-stack cleanup as well as the message — the group's screens stay
 * on the stack pointing at a group that no longer exists locally. Only the detail screen degrades
 * gracefully there; settle-up, for one, waits on a group that will never arrive. Accepted because
 * the window is small: the socket only runs while the app is in the foreground (`ConnectionGate`),
 * and the shell collects from `STARTED`, so the two are out of step only across a backgrounding.
 */
class GroupRemovalNotifier {
    private val _removals = MutableSharedFlow<RemovedFromGroup>(extraBufferCapacity = EVENT_BUFFER)
    val removals: SharedFlow<RemovedFromGroup> = _removals.asSharedFlow()

    fun notifyRemoved(
        groupId: String,
        title: String?,
    ) {
        _removals.tryEmit(RemovedFromGroup(groupId = groupId, title = title))
    }

    private companion object {
        private const val EVENT_BUFFER = 8
    }
}
