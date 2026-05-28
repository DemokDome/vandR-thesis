package com.domedemok.travelplanner.data.remote

/**
 * Firestore top-level and sub-collection name constants shared across all
 * repository implementations on every platform.
 *
 * Centralising these prevents silent drift when a collection is renamed and
 * makes it easy to audit which part of the schema each repo touches.
 */
object FirestoreCollections {
    // Top-level collections
    const val TRIPS = "trips"
    const val USERS = "users"

    // Sub-collections under trips/{tripId}/
    const val EXPENSES   = "expenses"
    const val PHOTOS     = "photos"
    const val AI_CHAT    = "ai_chat"
    const val GROUP_CHAT = "group_chat"
    const val ITINERARY  = "itinerary"
    const val PLACES     = "places"
}

/**
 * Pagination limits shared by the group-chat and AI-chat repositories
 * on both Android and JS platforms.
 */
object ChatLimits {
    /** Maximum number of messages fetched per real-time snapshot / one-shot query. */
    const val MESSAGE_LIMIT = 50
}
