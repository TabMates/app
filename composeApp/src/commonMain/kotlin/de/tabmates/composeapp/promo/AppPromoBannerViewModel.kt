package de.tabmates.composeapp.promo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

data class AppPromoBannerState(
    val isVisible: Boolean = false,
)

/**
 * Drives the "get the Android app" strip. Knows nothing about which platform it is running on —
 * the call site gates that, so this stays testable without a browser.
 */
@KoinViewModel
class AppPromoBannerViewModel(
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    val state: StateFlow<AppPromoBannerState> =
        appPreferencesRepository
            .androidAppPromoSnoozedUntil()
            .map { snoozedUntil ->
                AppPromoBannerState(isVisible = snoozedUntil == null || snoozedUntil <= Clock.System.now())
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                // Hidden until the stored snooze has been read: showing first and retracting a
                // frame later would flash the strip at everyone who already dismissed it.
                initialValue = AppPromoBannerState(),
            )

    fun onDismiss() {
        viewModelScope.launch {
            appPreferencesRepository.snoozeAndroidAppPromo(Clock.System.now() + SNOOZE_DURATION)
        }
    }

    internal companion object {
        // Long enough not to nag, short enough that someone who dismissed it before signing up
        // still gets a second offer.
        internal val SNOOZE_DURATION = 30.days
    }
}
