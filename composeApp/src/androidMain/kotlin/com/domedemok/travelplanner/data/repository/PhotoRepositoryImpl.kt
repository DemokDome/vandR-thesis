package com.domedemok.travelplanner.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.domedemok.travelplanner.data.model.TripPhoto
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.PhotoBusinessRules
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import androidx.core.graphics.scale

class PhotoRepositoryImpl(
    private val auth:      FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : PhotoRepository {

    // ── Firestore helpers ─────────────────────────────────────────────────────

    private fun photosRef(tripId: String) =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.PHOTOS)

    // ── Flow ──────────────────────────────────────────────────────────────────

    override fun getPhotosFlow(tripId: String): Flow<List<TripPhoto>> = callbackFlow {
        val listener = photosRef(tripId)
            .orderBy("uploadedAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val photos = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        TripPhoto(
                            id           = doc.id,
                            tripId       = tripId,
                            uploadedBy   = doc.getString("uploadedBy")   ?: "",
                            uploaderName = doc.getString("uploaderName") ?: "",
                            storageUrl   = doc.getString("storageUrl")   ?: "",
                            uploadedAt   = doc.getLong("uploadedAt")     ?: 0L,
                            caption      = doc.getString("caption")      ?: "",
                        )
                    }.getOrNull()
                }
                trySend(photos)
            }
        awaitClose { listener.remove() }
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    override suspend fun uploadPhoto(
        tripId:     String,
        imageBytes: ByteArray,
        caption:    String,
    ): Result<TripPhoto> = runCatching {
        val user         = auth.currentUser ?: error("Not logged in")
        val photoId      = UUID.randomUUID().toString()
        val uploaderName = PhotoBusinessRules.resolveUploaderName(user.displayName, user.email)

        // 1. Compress to JPEG ≤ MAX_IMAGE_DIMENSION_PX, quality JPEG_QUALITY
        val compressed = compressImage(imageBytes)

        // 2. Upload to Firebase Storage
        val storageRef = Firebase.storage.reference
            .child(PhotoBusinessRules.storagePathFor(tripId, photoId))
        storageRef.putBytes(compressed).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        // 3. Save metadata to Firestore
        val uploadedAt = System.currentTimeMillis()
        photosRef(tripId).document(photoId).set(
            PhotoBusinessRules.buildPhotoMetadataPayload(
                uploaderUid  = user.uid,
                uploaderName = uploaderName,
                downloadUrl  = downloadUrl,
                uploadedAtMs = uploadedAt,
                caption      = caption,
            )
        ).await()

        TripPhoto(
            id           = photoId,
            tripId       = tripId,
            uploadedBy   = user.uid,
            uploaderName = uploaderName,
            storageUrl   = downloadUrl,
            uploadedAt   = uploadedAt,
            caption      = caption,
        )
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    override suspend fun deletePhoto(
        tripId:     String,
        photoId:    String,
        storageUrl: String,
    ): Result<Unit> = runCatching {
        // Remove from Storage; ignore "object not found" (already deleted), re-throw anything else
        try {
            Firebase.storage.getReferenceFromUrl(storageUrl).delete().await()
        } catch (e: StorageException) {
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }

        // Remove Firestore metadata
        photosRef(tripId).document(photoId).delete().await()
    }

    // ── Compression ───────────────────────────────────────────────────────────

    /**
     * Scale the bitmap so neither dimension exceeds [maxSizePx], then encode
     * as JPEG at [quality] (0-100).  Returns the original bytes untouched if
     * decoding fails (e.g. the input is already WebP/PNG the decoder can't
     * read).
     */
    private fun compressImage(
        bytes:      ByteArray,
        maxSizePx:  Int = PhotoBusinessRules.MAX_IMAGE_DIMENSION_PX,
        quality:    Int = PhotoBusinessRules.JPEG_QUALITY,
    ): ByteArray {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return bytes  // couldn't decode — upload as-is

        val scale = minOf(
            1f,
            minOf(
                maxSizePx.toFloat() / original.width,
                maxSizePx.toFloat() / original.height,
            )
        )
        val scaled = if (scale < 1f) {
            original.scale((original.width * scale).toInt(), (original.height * scale).toInt())
        } else original

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)

        if (scaled !== original) scaled.recycle()
        original.recycle()

        return out.toByteArray()
    }
}
