package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Authentication operations backed by Firebase Auth, with the user's profile
 * mirrored into a Firestore `users/{uid}` document so the display name is
 * available to other clients (chat sender labels, share-trip lookups, etc.).
 *
 * The two `delete*` calls reflect Firebase's "recent login" rule:
 *  - [deleteAccount] succeeds only when the auth token is fresh.
 *  - [reauthenticateAndDelete] re-verifies the password first, so the call is
 *    safe even on long-lived sessions — used from the Settings screen.
 */
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean

    /**
     * Hot stream of the currently signed-in user, or `null` after sign-out.
     * Emits an immediate provisional [User] from the Firebase Auth token, then
     * a refined emission once the Firestore profile document is fetched.
     */
    fun getAuthState(): Flow<User?>

    suspend fun deleteAccount(): Result<Unit>
    suspend fun reauthenticateAndDelete(password: String): Result<Unit>

    /**
     * Triggers Firebase's hosted password-reset email flow for [email].
     * Returns success even if the address is unknown — Firebase intentionally
     * does not differentiate, to avoid leaking which emails are registered.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
