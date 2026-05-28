package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.User

/**
 * Pure decision logic for [com.domedemok.travelplanner.data.repository.AuthRepository].
 *
 * The orchestration sequences (Firebase listener wiring, deletion guard
 * flags) are necessarily stateful and therefore stay in the platform impls.
 * What lives here is the *data construction*: how do we merge auth fields
 * with Firestore profile fields into a [User], and what payload do we write
 * back to Firestore on sign-up.
 */
object AuthBusinessRules {

    /**
     * Resolves the canonical display name when merging an Auth identity with
     * a Firestore profile snapshot. Profile takes precedence (it's the source
     * of truth and survives auth-only refreshes); auth name is the fallback;
     * empty string if neither is set.
     */
    fun resolveDisplayName(profileName: String?, authDisplayName: String?): String =
        profileName?.takeIf { it.isNotBlank() }
            ?: authDisplayName?.takeIf { it.isNotBlank() }
            ?: ""

    /**
     * Resolves the canonical `createdAt` timestamp. Profile value wins because
     * the profile records the true sign-up moment; the auth account does not
     * preserve it across some recovery flows. Falls back to [nowMs] so a freshly
     * signed-up user without a profile doc still has a sensible value.
     */
    fun resolveCreatedAt(profileCreatedAt: Long?, nowMs: Long): Long =
        profileCreatedAt?.takeIf { it > 0L } ?: nowMs

    /**
     * Builds a [User] from the auth-side identity ([uid] / [authEmail] /
     * [authDisplayName]) overlaid with the optional Firestore profile fields.
     * Same merging rules apply whether the profile is fresh, stale, or absent.
     */
    fun buildUser(
        uid:                String,
        authEmail:          String?,
        authDisplayName:    String?,
        profileDisplayName: String?,
        profileCreatedAt:   Long?,
        nowMs:              Long,
    ): User = User(
        id          = uid,
        email       = authEmail ?: "",
        displayName = resolveDisplayName(profileDisplayName, authDisplayName),
        createdAt   = resolveCreatedAt(profileCreatedAt, nowMs),
    )

    /**
     * Provisional [User] from auth-only fields, emitted to the UI immediately
     * after sign-in so it can unblock without waiting for the Firestore profile
     * round-trip. `createdAt = 0L` is the sentinel meaning "not yet known"; the
     * refined emit will overwrite it once the profile snapshot arrives.
     */
    fun buildProvisionalUser(uid: String, authEmail: String?, authDisplayName: String?): User =
        User(
            id          = uid,
            email       = authEmail ?: "",
            displayName = authDisplayName ?: "",
            createdAt   = 0L,
        )

    /**
     * Field-shape for the `users/{uid}` Firestore doc written on sign-up.
     * Returned as a generic [Map] so each platform can hand it to its native
     * Firestore set/write API directly.
     */
    fun buildSignUpProfilePayload(user: User): Map<String, Any> = mapOf(
        "email"       to user.email,
        "displayName" to user.displayName,
        "createdAt"   to user.createdAt,
    )
}
