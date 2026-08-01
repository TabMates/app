package de.tabmates.features.authentication.presentation.environment

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.core.presentation.util.UiText

/** Which server the picker currently points at — not necessarily the one in use. */
enum class EnvironmentMode {
    DEFAULT,
    CUSTOM,
}

data class EnvironmentState(
    val urlTextFieldState: TextFieldState = TextFieldState(),
    val apiKeyTextFieldState: TextFieldState = TextFieldState(),
    /** Whether the custom row is the one in use — it wears the "Active" badge. */
    val isCustomActive: Boolean = false,
    /** URL the app falls back to, shown as the subtitle of the default row. */
    val defaultUrl: String = "",
    /** Last custom URL entered, subtitle of the custom row. Null while none was ever set up. */
    val storedCustomUrl: String? = null,
    val selectedMode: EnvironmentMode = EnvironmentMode.DEFAULT,
    val isApiKeyVisible: Boolean = false,
    val isApplying: Boolean = false,
    val canApply: Boolean = false,
    /**
     * Split per field so the message lands under the input it blames: a rejected api-key and an
     * unreachable host are told apart by which field is marked, not only by the wording. At most
     * one of the two is set — a switch fails for exactly one reason.
     */
    val urlError: UiText? = null,
    val apiKeyError: UiText? = null,
) {
    /**
     * Both rows share one button, so what enables it depends on the pick: a custom server needs a
     * filled-in form, while going back to the default is only a change when a custom one is live.
     */
    val canSubmit: Boolean
        get() =
            when (selectedMode) {
                EnvironmentMode.CUSTOM -> canApply
                EnvironmentMode.DEFAULT -> isCustomActive && !isApplying
            }
}
