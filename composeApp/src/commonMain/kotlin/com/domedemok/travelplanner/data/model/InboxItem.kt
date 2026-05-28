package com.domedemok.travelplanner.data.model

/**
 * Sealed hierarchy for all notification types shown in the Inbox tab.
 *
 * Each subtype carries the minimum information needed to render a card
 * without re-querying Firestore at display time.
 */
sealed class InboxItem {
    abstract val id: String
    abstract val tripId: String
    abstract val tripName: String
    abstract val timestamp: Long

    /** A chat message sent by another member of the trip. */
    data class ChatNotification(
        override val id: String,
        override val tripId: String,
        override val tripName: String,
        val senderName: String,
        val messagePreview: String,
        override val timestamp: Long,
    ) : InboxItem()

    /**
     * A place that was saved to the trip by another member.
     * [isDeleted] = true when the place has subsequently been removed —
     * the card stays visible with a "removed" style so nothing silently disappears.
     */
    data class PlaceNotification(
        override val id: String,
        override val tripId: String,
        override val tripName: String,
        val placeName: String,
        val category: String,
        override val timestamp: Long,
        val isDeleted: Boolean = false,
    ) : InboxItem()

    /**
     * An expense where the current user is listed in the debts.
     * [isDeleted] = true when the expense has subsequently been removed.
     */
    data class ExpenseNotification(
        override val id: String,
        override val tripId: String,
        override val tripName: String,
        val title: String,
        val amount: Double,
        val paidByName: String,
        override val timestamp: Long,
        val isDeleted: Boolean = false,
    ) : InboxItem()

    /**
     * A member joined or left the trip.
     * [isSelf] = true when the notification is directed at the current user
     * (e.g. "You were added to this trip").
     */
    data class MemberNotification(
        override val id: String,
        override val tripId: String,
        override val tripName: String,
        val memberName: String,
        val isJoined: Boolean,
        val isSelf: Boolean = false,
        override val timestamp: Long,
    ) : InboxItem()
}
