package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.User
import com.domedemok.travelplanner.data.repository.AuthRepository
import com.domedemok.travelplanner.data.repository.rules.AuthErrorCode
import com.domedemok.travelplanner.data.repository.rules.AuthErrorMapper
import com.domedemok.travelplanner.i18n.Strings
import com.domedemok.travelplanner.i18n.localizedMessage
import com.domedemok.travelplanner.util.SecurityValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Error surface shown to the auth UI.
 *
 * [Code] is for Firebase failures where the UI translates the symbolic code
 * to a localised, user-friendly message — no raw exception text leaks through.
 *
 * [Raw] carries an already-formed message (typically a client-side validation
 * result from [SecurityValidator]) that the UI can show verbatim.
 */
sealed class AuthUiError {
    data class Code(val code: AuthErrorCode) : AuthUiError()
    data class Raw(val message: String)      : AuthUiError()
}

/**
 * Resolves this UI error into a final, user-facing string. Codes go through
 * the localised [Strings] catalog; raw messages pass through unchanged.
 *
 * Centralised here so every auth-related screen renders the same code with
 * the same localised text — no per-screen translation drift.
 */
fun AuthUiError.resolve(strings: Strings): String = when (this) {
    is AuthUiError.Code -> code.localizedMessage(strings)
    is AuthUiError.Raw  -> message
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: AuthUiError? = null,
    val isLoggedIn: Boolean = false,
    /**
     * False until the first emission from getAuthState() arrives.
     * Android Firebase auth is synchronous so this flips nearly instantly; on JS/web,
     * Firebase restores auth from IndexedDB asynchronously and keeping NavDisplay
     * hidden until this is true prevents a Splash→Main ghost transition.
     */
    val isAuthStateKnown: Boolean = false,
    val requiresReAuthForDeletion: Boolean = false,
    /** Set after `sendPasswordResetEmail` succeeds; the UI reads it to swap the form for a confirmation. */
    val passwordResetEmailSent: Boolean = false,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // Pre-seed isLoggedIn from the synchronous cache so App.kt can pick the
    // correct startDestination on the first composition — avoids a Splash flash
    // for already-authenticated users.
    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = authRepository.isUserLoggedIn()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.getAuthState().collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser      = user,
                    isLoggedIn       = user != null,
                    isAuthStateKnown = true,   // latch: once true, never goes back to false
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        val cleanEmail = SecurityValidator.sanitize(email)

        validateOrSetError(SecurityValidator.validateEmail(cleanEmail)) ?: return
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = AuthUiError.Raw("Password cannot be empty."))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signIn(cleanEmail, password)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading   = false,
                        currentUser = user,
                        isLoggedIn  = true,
                        error       = null,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = AuthUiError.Code(AuthErrorMapper.fromException(e)),
                    )
                }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String, displayName: String) {
        val cleanEmail       = SecurityValidator.sanitize(email)
        val cleanDisplayName = SecurityValidator.sanitize(displayName)

        validateOrSetError(SecurityValidator.validateEmail(cleanEmail))         ?: return
        validateOrSetError(SecurityValidator.validateDisplayName(cleanDisplayName)) ?: return
        validateOrSetError(SecurityValidator.validatePassword(password))        ?: return

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = AuthUiError.Raw("Password fields don't match."))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signUp(cleanEmail, password, cleanDisplayName)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading   = false,
                        currentUser = user,
                        isLoggedIn  = true,
                        error       = null,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = AuthUiError.Code(AuthErrorMapper.fromException(e)),
                    )
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState(isAuthStateKnown = true)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.value = AuthUiState(isAuthStateKnown = true)
                }
                .onFailure { e ->
                    val msg = e.message.orEmpty()
                    if (msg.contains("RecentLoginRequired") || msg.contains("recent-login")) {
                        _uiState.value = _uiState.value.copy(
                            isLoading                 = false,
                            requiresReAuthForDeletion = true,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error     = AuthUiError.Code(AuthErrorMapper.fromException(e)),
                        )
                    }
                }
        }
    }

    fun confirmDeleteWithPassword(password: String) {
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = AuthUiError.Raw("Password cannot be empty!"))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.reauthenticateAndDelete(password)
                .onSuccess {
                    _uiState.value = AuthUiState(isAuthStateKnown = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        // Common case here is wrong password → INVALID_CREDENTIALS resolves to the right message.
                        error     = AuthUiError.Code(AuthErrorMapper.fromException(e)),
                    )
                }
        }
    }

    /**
     * Triggers Firebase's hosted password-reset email flow. Always reports success
     * to the UI even if the address is unknown — Firebase intentionally hides which
     * emails are registered, and we mirror that to avoid leaking the same info.
     */
    fun sendPasswordResetEmail(email: String) {
        val cleanEmail = SecurityValidator.sanitize(email)
        validateOrSetError(SecurityValidator.validateEmail(cleanEmail)) ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, passwordResetEmailSent = false)
            authRepository.sendPasswordResetEmail(cleanEmail)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, passwordResetEmailSent = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = AuthUiError.Code(AuthErrorMapper.fromException(e)),
                    )
                }
        }
    }

    fun cancelDeletion() {
        _uiState.value = _uiState.value.copy(requiresReAuthForDeletion = false, error = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearPasswordResetSent() {
        _uiState.value = _uiState.value.copy(passwordResetEmailSent = false)
    }

    /**
     * Convenience: when [errorMessage] is non-null, sets it on the state and returns
     * `null` so the caller can short-circuit with `?: return`. When valid, returns
     * a non-null sentinel so the caller proceeds.
     */
    private fun validateOrSetError(errorMessage: String?): Unit? {
        if (errorMessage != null) {
            _uiState.value = _uiState.value.copy(error = AuthUiError.Raw(errorMessage))
            return null
        }
        return Unit
    }
}
