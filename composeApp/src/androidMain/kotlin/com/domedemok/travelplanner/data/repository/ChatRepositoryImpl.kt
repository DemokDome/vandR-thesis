package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.ChatMessage
import com.domedemok.travelplanner.data.remote.ChatLimits
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.ChatBusinessRules
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Group chat persistence under `trips/{id}/group_chat`.
 *
 * Messages are written with all fields at the top level so Firestore's
 * automatic `toObject(ChatMessage::class.java)` deserialiser works directly —
 * [ChatMessage] has default values on every property.
 */
class ChatRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ChatRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val currentUserName: String?
        get() = auth.currentUser?.displayName

    private fun chatRef(tripId: String) =
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
                ChatBusinessRules.SendDecision.SenderIdMismatch ->
                    return Result.failure(Exception("Sender ID mismatch"))
                is ChatBusinessRules.SendDecision.Allowed -> {
                    // Allocate the doc ref up-front so its ID can be stored in the document body.
                    val docRef = chatRef(tripId).document()
                    docRef.set(
                        ChatBusinessRules.buildMessagePayload(
                            docId      = docRef.id,
                            tripId     = tripId,
                            message    = message,
                            senderName = decision.resolvedSenderName,
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMessagesFlow(tripId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = recentMessagesQuery(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }.reversed())
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getMessages(tripId: String): Result<List<ChatMessage>> = runCatching {
        recentMessagesQuery(tripId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(ChatMessage::class.java) }
            .reversed()
    }

    /** DESCENDING + limit: cheapest way to grab the tail of the conversation; reverse client-side for display. */
    private fun recentMessagesQuery(tripId: String): Query =
        chatRef(tripId)
            .orderBy(ChatBusinessRules.ORDER_FIELD, Query.Direction.DESCENDING)
            .limit(ChatLimits.MESSAGE_LIMIT.toLong())
}
