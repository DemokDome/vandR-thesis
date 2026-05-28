package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

/**
 * A travel plan owned by [createdBy] and optionally shared with other users.
 *
 * Membership is tracked in [tripMembers] (`userId → displayName`) — this is the
 * single source of truth for **active** members, used by both Firestore security
 * rules (`request.auth.uid in resource.data.tripMembers.keys()`) and the UI for
 * fast name-resolution without a separate `users/{uid}` lookup. The owner's UID
 * is always present in this map alongside any invited / joined members.
 *
 * [formerMembers] preserves the display name of users who have **left** the
 * trip (or were removed by the owner) so historical expense splits and chat
 * messages still render their name. This map is read-only from the security
 * rules' perspective — it never grants access. Use [displayNameFor] for a
 * single combined lookup over both maps.
 */
@Serializable
data class Trip(
    val id:            String              = "",
    val name:          String              = "",
    val description:   String              = "",
    val destination:   String              = "",   // primary destination city/region
    val createdBy:     String              = "",   // Firebase Auth UID of the owner
    val tripMembers:   Map<String, String> = emptyMap(),
    val formerMembers: Map<String, String> = emptyMap(),
    val startDate:     Long                = 0L,
    val endDate:       Long                = 0L,
    val createdAt:     Long                = 0L,
    val updatedAt:     Long                = 0L,
    val lastOpenedAt:  Long                = 0L,   // updated on every view, not just edits
    val joinCode:      String              = "",   // 6-char alphanumeric code (QR / share link)
) {
    /**
     * Looks up a member's display name across both active and former members.
     * Returns `null` if the UID has never been associated with this trip.
     */
    fun displayNameFor(uid: String): String? = tripMembers[uid] ?: formerMembers[uid]
}
