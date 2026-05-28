package com.domedemok.travelplanner.data.repository.rules

/**
 * Pure, platform-independent decision logic for [com.domedemok.travelplanner.data.repository.TripRepository].
 *
 * Operates exclusively on plain Kotlin types (Strings, Maps, Sets). The
 * platform implementations are responsible for:
 *  - Parsing the raw Firestore document into the inputs of these functions.
 *  - Translating the returned `*Decision` into actual Firestore writes
 *    via their own SDK.
 *
 * Every function here is referentially transparent and trivially unit
 * testable — see `TripBusinessRulesTest` in `commonTest`.
 */
object TripBusinessRules {

    /** Outcome of a join-by-code attempt before any Firestore write happens. */
    sealed class JoinDecision {
        /** The user is already the owner or a current member — no write needed. */
        data object AlreadyMember : JoinDecision()

        /** Caller is permitted to join; impl should write the membership entry. */
        data class Allowed(val joinerId: String, val joinerName: String) : JoinDecision()
    }

    /**
     * Decides whether a sign-in user is allowed to join a trip via its join code.
     *
     * @param tripCreatorId     The trip's owner uid (from `createdBy`).
     * @param existingMemberIds Current `tripMembers` keys — owner is included.
     * @param joinerId          The uid attempting to join.
     * @param joinerName        Display name the impl will write under `tripMembers.{joinerId}`.
     */
    fun evaluateJoinAttempt(
        tripCreatorId:     String,
        existingMemberIds: Set<String>,
        joinerId:          String,
        joinerName:        String,
    ): JoinDecision = when {
        joinerId == tripCreatorId     -> JoinDecision.AlreadyMember
        joinerId in existingMemberIds -> JoinDecision.AlreadyMember
        else                           -> JoinDecision.Allowed(joinerId, joinerName)
    }

    /** Outcome of an email-based share attempt. */
    sealed class ShareDecision {
        /** Caller tried to share the trip with themselves. */
        data object SelfShare : ShareDecision()

        /** The target user is already a current member of the trip. */
        data object AlreadyShared : ShareDecision()

        /** Caller may share; impl should write the membership entry. */
        data class Allowed(val targetUserId: String, val targetUserName: String) : ShareDecision()
    }

    /**
     * Decides whether the current user may share the trip with [targetUserId].
     *
     * @param sharerUserId      The uid of the user initiating the share.
     * @param targetUserId      The uid being added.
     * @param existingMemberIds Current `tripMembers` keys.
     * @param targetUserName    Display name to write under `tripMembers.{targetUserId}`.
     */
    fun evaluateShareAttempt(
        sharerUserId:      String,
        targetUserId:      String,
        existingMemberIds: Set<String>,
        targetUserName:    String,
    ): ShareDecision = when {
        sharerUserId == targetUserId      -> ShareDecision.SelfShare
        targetUserId in existingMemberIds -> ShareDecision.AlreadyShared
        else                               -> ShareDecision.Allowed(targetUserId, targetUserName)
    }

    /** Outcome of an owner-removing-member attempt. */
    sealed class RemoveMemberDecision {
        /** Only the trip owner may remove members. */
        data object NotOwner : RemoveMemberDecision()

        /** Removal allowed; impl should delete from tripMembers and write to formerMembers. */
        data class Allowed(
            val removedUserId:           String,
            val preservedDisplayName:    String,
        ) : RemoveMemberDecision()
    }

    /**
     * Decides whether the current user may remove [memberIdToRemove] from the trip.
     * Also resolves the display name that should be archived into `formerMembers`
     * so historical expense splits still render the leaver's name.
     */
    fun evaluateRemoveMember(
        removerId:             String,
        tripCreatorId:         String,
        existingMembers:       Map<String, String>,
        memberIdToRemove:      String,
        fallbackDisplayName:   String = UNKNOWN_DISPLAY_NAME,
    ): RemoveMemberDecision =
        if (removerId != tripCreatorId) {
            RemoveMemberDecision.NotOwner
        } else {
            RemoveMemberDecision.Allowed(
                removedUserId        = memberIdToRemove,
                preservedDisplayName = existingMembers[memberIdToRemove] ?: fallbackDisplayName,
            )
        }

    /**
     * Resolves the display name to archive when a user voluntarily leaves a trip.
     *
     * Prefers the name stored in `tripMembers` (which may differ from the user's
     * current auth display name due to per-trip renames), then falls back to the
     * auth display name, then to a fixed sentinel.
     */
    fun resolveLeaverDisplayName(
        currentMembers:       Map<String, String>,
        leaverId:             String,
        authDisplayName:      String?,
        fallbackDisplayName:  String = UNKNOWN_DISPLAY_NAME,
    ): String = currentMembers[leaverId]
        ?: authDisplayName?.takeIf { it.isNotBlank() }
        ?: fallbackDisplayName

    /** Sentinel for missing display names. Mirrors `UNKNOWN_NAME` in both repository impls. */
    const val UNKNOWN_DISPLAY_NAME: String = "Unknown"
}
