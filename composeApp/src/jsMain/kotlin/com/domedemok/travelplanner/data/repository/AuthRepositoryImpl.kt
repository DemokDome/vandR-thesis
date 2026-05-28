package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.User
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.AuthBusinessRules
import com.domedemok.travelplanner.util.getCurrentTimestamp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * JS / Wasm implementation of [AuthRepository] using the GitLive Firebase wrapper.
 *
 * Two web-specific quirks of note:
 *  - Firestore numeric fields written from Kotlin/JS round-trip as `Double`,
 *    not `Long` (Kotlin Long is a two-Int struct in JS that the Firebase SDK
 *    rejects). All `createdAt` reads cast through `Double`.
 *  - There is no native re-authentication helper exposed in GitLive, so
 *    [reauthenticateAndDelete] re-signs in with email+password to refresh the
 *    auth token before the destructive call.
 */
class AuthRepositoryImpl : AuthRepository {

    private val auth      = Firebase.auth
    private val firestore = Firebase.firestore

    // Re-entrancy guards: prevent the auth-state Firestore listener from
    // emitting transient null states while we're mid-registration or mid-deletion.
    private var isRegistering    = false
    private var isDeletingAccount = false

    private fun usersDoc(uid: String) = firestore.collection(FirestoreCollections.USERS).document(uid)

    /** Reads `displayName` from a Firestore profile snapshot, falling back to null on any error. */
    private fun readDisplayName(snapshot: DocumentSnapshot): String? =
        runCatching { snapshot.get<String>("displayName") }.getOrNull()

    /** Reads `createdAt` as Double then narrows to Long — see class KDoc on the JS Long quirk. */
    private fun readCreatedAt(snapshot: DocumentSnapshot): Long? =
        runCatching { snapshot.get<Double>("createdAt").toLong() }.getOrNull()

    /** Builds a [User] from auth fields only — used as the offline / fallback path. */
    private fun FirebaseUser.toAuthOnlyUser(): User = AuthBusinessRules.buildUser(
        uid                = uid,
        authEmail          = email,
        authDisplayName    = displayName,
        profileDisplayName = null,
        profileCreatedAt   = null,
        nowMs              = getCurrentTimestamp(),
    )

    /** Builds a [User] by overlaying a Firestore profile snapshot over the Auth fields. */
    private fun FirebaseUser.toUser(snapshot: DocumentSnapshot): User = AuthBusinessRules.buildUser(
        uid                = uid,
        authEmail          = email,
        authDisplayName    = displayName,
        profileDisplayName = readDisplayName(snapshot),
        profileCreatedAt   = readCreatedAt(snapshot),
        nowMs              = getCurrentTimestamp(),
    )

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val firebaseUser = auth.signInWithEmailAndPassword(email, password).user
                ?: return Result.failure(Exception("User is null"))

            val user = runCatching {
                val doc = usersDoc(firebaseUser.uid).get()
                if (doc.exists) firebaseUser.toUser(doc) else firebaseUser.toAuthOnlyUser()
            }.getOrElse { firebaseUser.toAuthOnlyUser() }

            Result.success(user)
        } catch (e: Exception) {
            console.error("SignIn failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return runCatching {
            val doc = usersDoc(firebaseUser.uid).get()
            if (doc.exists) firebaseUser.toUser(doc) else null
        }.getOrNull()
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            isRegistering = true
            val firebaseUser = auth.createUserWithEmailAndPassword(email, password).user
                ?: return Result.failure(Exception("User is null"))

            firebaseUser.updateProfile(displayName = displayName)

            val user = User(
                id          = firebaseUser.uid,
                email       = email,
                displayName = displayName,
                createdAt   = getCurrentTimestamp(),
            )

            // JS Firestore needs Double for the 64-bit Long timestamp — see class KDoc.
            usersDoc(firebaseUser.uid).set(
                AuthBusinessRules.buildSignUpProfilePayload(user)
                    .toMutableMap()
                    .apply { put("createdAt", user.createdAt.toDouble()) }
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            isRegistering = false
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching { auth.signOut() }

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override fun getAuthState(): Flow<User?> = channelFlow {
        auth.authStateChanged.collectLatest { firebaseUser ->
            if (firebaseUser == null) { send(null); return@collectLatest }

            // Provisional emit from Auth-only fields so the UI can unblock immediately.
            send(AuthBusinessRules.buildProvisionalUser(
                uid             = firebaseUser.uid,
                authEmail       = firebaseUser.email,
                authDisplayName = firebaseUser.displayName,
            ))

            try {
                usersDoc(firebaseUser.uid).snapshots.collect { snapshot ->
                    if (isRegistering || isDeletingAccount) return@collect
                    if (snapshot.exists) send(firebaseUser.toUser(snapshot))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // structured concurrency: cancellation must propagate
            } catch (e: Exception) {
                console.warn("Firestore listener error: ${e.message}")
            }
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withDeletionGuard {
        val firebaseUser = auth.currentUser ?: error("Not logged in")
        deleteProfileAndAccount(firebaseUser)
    }

    override suspend fun reauthenticateAndDelete(password: String): Result<Unit> = withDeletionGuard {
        val firebaseUser = auth.currentUser ?: error("Not logged in")
        val email = firebaseUser.email ?: error("No email found")

        // GitLive web wrapper does not expose `reauthenticate(credential)`, so we
        // refresh the token by re-signing in with the same credentials. This is
        // safe: the email is bound to the current account, so a wrong password
        // simply fails — there's no risk of switching identity.
        auth.signInWithEmailAndPassword(email, password)

        val refreshed = auth.currentUser ?: error("User lost after reauth")
        deleteProfileAndAccount(refreshed)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private inline fun withDeletionGuard(block: () -> Unit): Result<Unit> {
        return try {
            isDeletingAccount = true
            block()
            Result.success(Unit)
        } catch (e: Exception) {
            console.error("Delete account failed", e)
            Result.failure(e)
        } finally {
            isDeletingAccount = false
        }
    }

    private suspend fun deleteProfileAndAccount(firebaseUser: FirebaseUser) {
        // Best-effort Firestore cleanup — orphaned profile docs can be reaped server-side
        // if this fails, but the Auth account must still be removed below.
        runCatching { usersDoc(firebaseUser.uid).delete() }
            .onFailure { console.warn("User document delete failed: ${it.message}") }

        firebaseUser.delete()
        auth.signOut()
    }
}
