package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.User
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.AuthBusinessRules
import com.domedemok.travelplanner.util.getCurrentTimestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Android implementation of [AuthRepository] backed by Firebase Auth.
 *
 * The user profile (display name, createdAt) is mirrored into a Firestore
 * `users/{uid}` document so other clients can resolve names without inheriting
 * the current user's auth state. Sign-in is intentionally lenient: if the
 * Firestore lookup fails (offline / slow), we still sign the user in with the
 * Firebase Auth profile as a fallback so they're not locked out of the app.
 */
class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    // Re-entrancy guards: while these flags are set, the auth-state Firestore
    // listener must not fire `trySend(...)` — otherwise a partially-deleted
    // profile briefly looks like a logged-in user with no display name.
    private var isRegistering = false
    private var isDeletingAccount = false

    private fun usersDoc(uid: String) = firestore.collection(FirestoreCollections.USERS).document(uid)

    /** Builds a [User] from Firebase Auth, optionally overlaying a Firestore profile snapshot. */
    private fun FirebaseUser.toUser(profile: DocumentSnapshot? = null): User =
        AuthBusinessRules.buildUser(
            uid                = uid,
            authEmail          = email,
            authDisplayName    = displayName,
            profileDisplayName = profile?.getString("displayName"),
            profileCreatedAt   = profile?.getLong("createdAt"),
            nowMs              = getCurrentTimestamp(),
        )

    /**
     * Fetches the Firestore profile for [user] and builds a complete [User], or falls back
     * to the auth-only [User] when the Firestore lookup throws.
     *
     * Caveat: this collapses two distinct error modes into the same fallback path —
     * a transient Firestore failure (offline, network blip) and a "profile genuinely
     * missing" state (e.g. the profile doc was deleted while the auth account still
     * exists, possible if a delete-account flow runs offline). The thesis app only
     * meaningfully hits this codepath in the offline-delete race, so the lenient
     * fallback is intentionally retained. If richer error reporting is added later,
     * split into a sealed `SignInResult { Success / TransientError / ProfileMissing }`.
     */
    private suspend fun loadProfile(user: FirebaseUser): User = runCatching {
        val doc = usersDoc(user.uid).get().await()
        user.toUser(profile = doc.takeIf { it.exists() })
    }.getOrElse { user.toUser() }

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val firebaseUser = auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("User is null")
        loadProfile(firebaseUser)
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        // Returning null on a missing profile preserves the original behaviour:
        // callers treat "no Firestore doc" as "not really signed in yet".
        return runCatching {
            val doc = usersDoc(firebaseUser.uid).get().await()
            if (doc.exists()) firebaseUser.toUser(profile = doc) else null
        }.getOrNull()
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            isRegistering = true
            val firebaseUser = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: return Result.failure(Exception("User is null"))

            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
            ).await()

            val user = User(
                id          = firebaseUser.uid,
                email       = email,
                displayName = displayName,
                createdAt   = getCurrentTimestamp(),
            )

            usersDoc(firebaseUser.uid).set(AuthBusinessRules.buildSignUpProfilePayload(user)).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            isRegistering = false
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching { auth.signOut() }

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override fun getAuthState(): Flow<User?> = callbackFlow {
        var firestoreListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                firestoreListener?.remove()
                trySend(null)
                return@AuthStateListener
            }

            // 1. Provisional emit from the Auth token so the UI unblocks immediately.
            trySend(AuthBusinessRules.buildProvisionalUser(
                uid             = firebaseUser.uid,
                authEmail       = firebaseUser.email,
                authDisplayName = firebaseUser.displayName,
            ))

            // 2. Subscribe to the Firestore profile doc for the refined emit.
            firestoreListener?.remove()
            firestoreListener = usersDoc(firebaseUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (isRegistering || isDeletingAccount) return@addSnapshotListener
                    if (error != null) return@addSnapshotListener        // keep the user signed in on transient errors
                    if (snapshot != null && snapshot.exists()) {
                        trySend(firebaseUser.toUser(profile = snapshot))
                    }
                }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListener?.remove()
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withDeletionGuard {
        val firebaseUser = auth.currentUser ?: error("Not logged in")
        deleteProfileAndAccount(firebaseUser)
    }

    override suspend fun reauthenticateAndDelete(password: String): Result<Unit> = withDeletionGuard {
        val firebaseUser = auth.currentUser ?: error("Not logged in")
        val email = firebaseUser.email ?: error("No email found")

        val credential = EmailAuthProvider.getCredential(email, password)
        firebaseUser.reauthenticate(credential).await()

        deleteProfileAndAccount(firebaseUser)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private inline fun withDeletionGuard(block: () -> Unit): Result<Unit> {
        // `block` is invoked inline so it can call suspend helpers without an explicit suspend lambda type.
        return try {
            isDeletingAccount = true
            block()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            isDeletingAccount = false
        }
    }

    private suspend fun deleteProfileAndAccount(firebaseUser: FirebaseUser) {
        // Best-effort Firestore cleanup — if it fails the auth account is still
        // removed below, and the orphaned profile can be reaped server-side.
        runCatching { usersDoc(firebaseUser.uid).delete().await() }
        firebaseUser.delete().await()
        auth.signOut()
    }

}
