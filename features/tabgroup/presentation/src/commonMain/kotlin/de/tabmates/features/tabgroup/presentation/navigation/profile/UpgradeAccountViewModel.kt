package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.domain.validation.PasswordValidator
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
import de.tabmates.features.authentication.domain.EmailValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_email_invalid
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_error_email_in_use
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_password_mismatch
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_password_requirements
import tabmatesapp.features.tabgroup.presentation.generated.resources.upgrade_account_resend_needs_details

/**
 * Turns the signed-in anonymous account into a registered one.
 *
 * Submitting only asks the server to send a verification link — the account stays anonymous, and
 * the session stays valid, until that link is redeemed. Nothing here signs the user out: an
 * anonymous account has no password, so losing the session before the new credentials work would
 * strand it for good.
 */
@KoinViewModel
class UpgradeAccountViewModel(
    private val authService: AuthService,
) : ViewModel() {
    val emailState = TextFieldState()
    val passwordState = TextFieldState()
    val confirmPasswordState = TextFieldState()

    private val _state = MutableStateFlow(UpgradeAccountState())
    val state: StateFlow<UpgradeAccountState> = _state.asStateFlow()

    private val eventChannel = Channel<UpgradeAccountEvent>()
    val events = eventChannel.receiveAsFlow()

    private val emailInvalid = UiText.Resource(Res.string.upgrade_account_email_invalid)
    private val passwordRequirements = UiText.Resource(Res.string.upgrade_account_password_requirements)
    private val passwordMismatch = UiText.Resource(Res.string.upgrade_account_password_mismatch)

    init {
        // A request survives process death server-side, so re-open the screen in whichever phase
        // the account is actually in rather than showing an empty form over a live request.
        viewModelScope.launch {
            authService.refreshAccount().onSuccess { account ->
                if (account.user.userType == UserType.REGISTERED) {
                    eventChannel.send(UpgradeAccountEvent.AlreadyRegistered)
                } else {
                    // Only an anonymous account gets here, so a pending address can only be the
                    // migration target — for a registered one the same field means a pending
                    // change of an existing address.
                    _state.update { it.copy(pendingEmail = account.pendingEmail) }
                }
            }
        }
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun validateEmailOnBlur() {
        val email = emailState.text.toString().trim()
        if (email.isEmpty()) return
        _state.update {
            it.copy(emailError = if (EmailValidator.validate(email)) null else emailInvalid)
        }
    }

    fun validatePasswordOnBlur() {
        val password = passwordState.text.toString()
        if (password.isEmpty()) return
        _state.update {
            it.copy(passwordError = if (PasswordValidator.validate(password)) null else passwordRequirements)
        }
    }

    fun validateConfirmPasswordOnBlur() {
        val confirmPassword = confirmPasswordState.text.toString()
        if (confirmPassword.isEmpty()) return
        val matches = confirmPassword == passwordState.text.toString()
        _state.update {
            it.copy(confirmPasswordError = if (matches) null else passwordMismatch)
        }
    }

    fun onSubmit() {
        if (_state.value.isSubmitting) return
        if (!validateFormInputs()) return
        submit(
            email = emailState.text.toString().trim(),
            password = passwordState.text.toString(),
        )
    }

    /**
     * Re-sends the link by repeating the request, which the server treats as a replacement.
     *
     * The password is only held in the text field, so after process death there is nothing left to
     * repeat — fall back to the form rather than sending a link for a password the user no longer
     * knows they set.
     */
    fun onResend() {
        if (_state.value.isSubmitting) return
        val email = emailState.text.toString().trim()
        val password = passwordState.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(pendingEmail = null) }
            send(UpgradeAccountEvent.Error(UiText.Resource(Res.string.upgrade_account_resend_needs_details)))
            return
        }
        submit(email = email, password = password)
    }

    /** Returns to the form so a typo in the address can be corrected. */
    fun onUseDifferentEmail() {
        _state.update { it.copy(pendingEmail = null) }
    }

    private fun submit(
        email: String,
        password: String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            authService
                .migrateToRegistered(email = email, password = password)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false, pendingEmail = email) }
                    eventChannel.send(UpgradeAccountEvent.VerificationSent(email))
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(UpgradeAccountEvent.Error(error.toScreenUiText()))
                }
        }
    }

    private fun validateFormInputs(): Boolean {
        val email = emailState.text.toString().trim()
        val password = passwordState.text.toString()
        val confirmPassword = confirmPasswordState.text.toString()

        val emailError = if (EmailValidator.validate(email)) null else emailInvalid
        val passwordError = if (PasswordValidator.validate(password)) null else passwordRequirements
        val confirmPasswordError = if (confirmPassword == password) null else passwordMismatch

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
            )
        }
        return emailError == null && passwordError == null && confirmPasswordError == null
    }

    private fun DataError.Remote.toScreenUiText(): UiText {
        return when (this) {
            // Deliberately no "sign in instead" shortcut: signing in as the other account would
            // wipe this device's data and the anonymous account has no password to come back with.
            DataError.Remote.CONFLICT -> UiText.Resource(Res.string.upgrade_account_error_email_in_use)

            else -> toUiText()
        }
    }

    private fun send(event: UpgradeAccountEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
