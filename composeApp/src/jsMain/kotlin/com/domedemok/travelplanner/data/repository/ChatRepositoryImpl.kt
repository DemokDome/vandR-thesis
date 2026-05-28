package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.remote.ChatLimits
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.ChatBusinessRules
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * JS implementation of [ChatRepository].
 *
 * Writes go through a hand-rolled map because Kotlin Long is a two-Int struct
 * in JS and the Firestore SDK rejects it — `timestamp` is sent as Double.
 * Reads still use the auto-deserialiser since [ChatMessage] has Long defaults
 * that GitLive maps from the Double field cleanly.
 */
class ChatRepositoryImpl : ChatRepository {

    private val auth      = Firebase.auth
    private val firestore = Firebase.firestore

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val currentUserName: String?
        get() = auth.currentUser?.displayName

    private fun chatRef(tripId: String): CollectionReference =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.GROUP_CHAT)

    override suspend fun sendMessage(tripId: String, message: ChatMessage): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            // Pure rule: authorize sender + resolve the display name to write.
            val decision = ChatBusinessRules.validateAndPrepareSend(
                messageSenderId = message.senderId,
                currentUserId   = userId,
                currentUserName = currentUserName,
            )

            when (decision) {
                ChatBusinessRules.SendDecision.SenderIdMismatch -> {
                    console.error("ChatRepository: Sender ID mismatch!")
                    return Result.failure(Exception("Sender ID mismatch"))
                }
                is ChatBusinessRules.SendDecision.Allowed -> {
                    val docRef = chatRef(tripId).document
                    // JS Firestore needs Double for the 64-bit timestamp — patch
                    // the shared payload after the rule applies the Long value.
                    val payload = ChatBusinessRules.buildMessagePayload(
                        docId      = docRef.id,
                        tripId     = tripId,
                        message    = message,
                        senderName = decision.resolvedSenderName,
                    ).toMutableMap().apply { put("timestamp", message.timestamp.toDouble()) }
                    docRef.set(payload)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            console.error("ChatRepository: sendMessage failed:", e)
            Result.failure(e)
        }
    }

    override fun getMessagesFlow(tripId: String): Flow<List<ChatMessage>> =
        recentMessagesQuery(tripId).snapshots.map { snapshot ->
            snapshot.documents.map { it.data<ChatMessage>() }.reversed()
        }

    override suspend fun getMessages(tripId: String): Result<List<ChatMessage>> = runCatching {
        recentMessagesQuery(tripId).get()
            .documents
            .map { it.data<ChatMessage>() }
            .reversed()
    }

    /** DESCENDING + limit = cheapest tail-of-conversation; reverse client-side for display. */
    private fun recentMessagesQuery(tripId: String): Query =
        chatRef(tripId)
            .orderBy(ChatBusinessRules.ORDER_FIELD, Direction.DESCENDING)
            .limit(ChatLimits.MESSAGE_LIMIT)
}
