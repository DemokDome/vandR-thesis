package com.domedemok.travelplanner.data.repository.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthErrorMapperTest {

    private fun map(message: String): AuthErrorCode =
        AuthErrorMapper.fromException(Exception(message))

    // ── Email-already-in-use ────────────────────────────────────────────────

    @Test
    fun `JS dash-style email-already-in-use maps to EMAIL_ALREADY_IN_USE`() {
        assertEquals(
            AuthErrorCode.EMAIL_ALREADY_IN_USE,
            map("FirebaseError: Firebase: The email address is already in use by another account. (auth/email-already-in-use)."),
        )
    }

    @Test
    fun `Android underscore-style ERROR_EMAIL_ALREADY_IN_USE maps to EMAIL_ALREADY_IN_USE`() {
        assertEquals(
            AuthErrorCode.EMAIL_ALREADY_IN_USE,
            map("ERROR_EMAIL_ALREADY_IN_USE: The email is already taken"),
        )
    }

    // ── Invalid credentials (sign-in: wrong password / unknown email) ───────

    @Test
    fun `JS auth-invalid-credential maps to INVALID_CREDENTIALS`() {
        assertEquals(
            AuthErrorCode.INVALID_CREDENTIALS,
            map("Firebase: Invalid login credentials. (auth/invalid-credential)."),
        )
    }

    @Test
    fun `JS auth-wrong-password maps to INVALID_CREDENTIALS`() {
        assertEquals(
            AuthErrorCode.INVALID_CREDENTIALS,
            map("Firebase: (auth/wrong-password)."),
        )
    }

    @Test
    fun `Android ERROR_WRONG_PASSWORD maps to INVALID_CREDENTIALS`() {
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, map("ERROR_WRONG_PASSWORD"))
    }

    @Test
    fun `Android ERROR_INVALID_CREDENTIAL maps to INVALID_CREDENTIALS`() {
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, map("ERROR_INVALID_CREDENTIAL"))
    }

    // ── Weak password ───────────────────────────────────────────────────────

    @Test
    fun `weak-password message maps to WEAK_PASSWORD`() {
        assertEquals(
            AuthErrorCode.WEAK_PASSWORD,
            map("Firebase: Password should be at least 6 characters (auth/weak-password)."),
        )
        assertEquals(AuthErrorCode.WEAK_PASSWORD, map("ERROR_WEAK_PASSWORD"))
    }

    // ── Invalid email ───────────────────────────────────────────────────────

    @Test
    fun `invalid-email message maps to INVALID_EMAIL`() {
        assertEquals(
            AuthErrorCode.INVALID_EMAIL,
            map("Firebase: (auth/invalid-email)."),
        )
        assertEquals(AuthErrorCode.INVALID_EMAIL, map("ERROR_INVALID_EMAIL"))
    }

    // ── User not found ──────────────────────────────────────────────────────

    @Test
    fun `user-not-found message maps to USER_NOT_FOUND`() {
        assertEquals(
            AuthErrorCode.USER_NOT_FOUND,
            map("Firebase: There is no user record (auth/user-not-found)."),
        )
        assertEquals(AuthErrorCode.USER_NOT_FOUND, map("ERROR_USER_NOT_FOUND"))
    }

    // ── User disabled ───────────────────────────────────────────────────────

    @Test
    fun `user-disabled message maps to USER_DISABLED`() {
        assertEquals(AuthErrorCode.USER_DISABLED, map("Firebase: (auth/user-disabled)."))
        assertEquals(AuthErrorCode.USER_DISABLED, map("ERROR_USER_DISABLED"))
    }

    // ── Network ─────────────────────────────────────────────────────────────

    @Test
    fun `network-request-failed maps to NETWORK_ERROR`() {
        assertEquals(AuthErrorCode.NETWORK_ERROR, map("Firebase: (auth/network-request-failed)."))
        assertEquals(AuthErrorCode.NETWORK_ERROR, map("ERROR_NETWORK_REQUEST_FAILED"))
    }

    // ── Rate limit ──────────────────────────────────────────────────────────

    @Test
    fun `too-many-requests maps to TOO_MANY_REQUESTS`() {
        assertEquals(AuthErrorCode.TOO_MANY_REQUESTS, map("Firebase: (auth/too-many-requests)."))
        assertEquals(AuthErrorCode.TOO_MANY_REQUESTS, map("ERROR_TOO_MANY_REQUESTS"))
    }

    // ── Unknown / fallback ──────────────────────────────────────────────────

    @Test
    fun `unrecognised message maps to UNKNOWN`() {
        assertEquals(AuthErrorCode.UNKNOWN, map("Some random error"))
        assertEquals(AuthErrorCode.UNKNOWN, map(""))
    }

    @Test
    fun `null exception message maps to UNKNOWN`() {
        val exception = object : Exception() { override val message: String? = null }
        assertEquals(AuthErrorCode.UNKNOWN, AuthErrorMapper.fromException(exception))
    }

    // ── Case-insensitivity ──────────────────────────────────────────────────

    @Test
    fun `matching is case-insensitive`() {
        assertEquals(AuthErrorCode.EMAIL_ALREADY_IN_USE, map("EMAIL-ALREADY-IN-USE in upper case"))
        assertEquals(AuthErrorCode.NETWORK_ERROR,        map("Network-Request-Failed mixed case"))
    }
}
