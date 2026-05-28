package com.domedemok.travelplanner.data.repository.rules

/**
 * User-facing categories of Firebase Auth failure.
 *
 * Decoupling the platform exception from the UI message means:
 *  - The UI can pick a friendly, localised string instead of leaking raw
 *    "FirebaseAuthInvalidCredentialsException" text.
 *  - The same code resolves the same message on Android and JS, even though
 *    the underlying exception classes are completely different.
 */
enum class AuthErrorCode {
    /** Email / password combination didn't match an account, or credentials malformed. */
    INVALID_CREDENTIALS,

    /** Sign-up: an account already exists with this email. */
    EMAIL_ALREADY_IN_USE,

    /** Sign-up: password doesn't meet Firebase's strength requirements. */
    WEAK_PASSWORD,

    /** Email string isn't a valid address (`@` missing, etc.). */
    INVALID_EMAIL,

    /** Sign-in: no account with this email. */
    USER_NOT_FOUND,

    /** Account has been disabled by an admin. */
    USER_DISABLED,

    /** Network / connectivity failure — usually retriable. */
    NETWORK_ERROR,

    /** Firebase rate-limit hit (too many failed attempts in a short window). */
    TOO_MANY_REQUESTS,

    /** Anything we don't recognise — caller should fall back to a generic message. */
    UNKNOWN,
}

/**
 * Pure mapping from a Firebase / GitLive Auth exception to a platform-independent
 * [AuthErrorCode]. Both SDKs surface the Firebase error identifier in the
 * exception message (Android: `ERROR_X` style, JS: `auth/x` style), so a
 * lowercase substring match catches both reliably without needing platform-specific
 * exception introspection.
 *
 * Unit-tested in `AuthErrorMapperTest` so a regression in the message-extraction
 * patterns (e.g. Firebase changing one of the error strings) is caught at build
 * time rather than at runtime.
 */
object AuthErrorMapper {

    fun fromException(throwable: Throwable): AuthErrorCode {
        val haystack = throwable.message?.lowercase().orEmpty()
        return when {
            haystack.containsAny("email-already-in-use", "email_already_in_use")
                -> AuthErrorCode.EMAIL_ALREADY_IN_USE

            haystack.containsAny(
                "invalid-credential",  "invalid_credential",
                "wrong-password",      "wrong_password",
                "invalid-login-credentials",
            ) -> AuthErrorCode.INVALID_CREDENTIALS

            haystack.containsAny("weak-password", "weak_password")
                -> AuthErrorCode.WEAK_PASSWORD

            haystack.containsAny("invalid-email", "invalid_email")
                -> AuthErrorCode.INVALID_EMAIL

            haystack.containsAny("user-not-found", "user_not_found")
                -> AuthErrorCode.USER_NOT_FOUND

            haystack.containsAny("user-disabled", "user_disabled")
                -> AuthErrorCode.USER_DISABLED

            haystack.containsAny("network-request-failed", "network_request_failed")
                -> AuthErrorCode.NETWORK_ERROR

            haystack.containsAny("too-many-requests", "too_many_requests")
                -> AuthErrorCode.TOO_MANY_REQUESTS

            else -> AuthErrorCode.UNKNOWN
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { contains(it) }
}
