package com.domedemok.travelplanner.data.repository

import com.domedemok.travelplanner.data.model.TripPhoto
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.domedemok.travelplanner.data.repository.rules.PhotoBusinessRules
import com.domedemok.travelplanner.util.generateUniqueId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.File
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.storageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.js.Date
import kotlinx.browser.document
import org.khronos.webgl.Uint8Array

class PhotoRepositoryImpl : PhotoRepository {

    private val auth      = Firebase.auth
    private val firestore = Firebase.firestore
    private val storage   = Firebase.storage

    // ── Firestore helpers ─────────────────────────────────────────────────────

    private fun photosCol(tripId: String) =
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).collection(FirestoreCollections.PHOTOS)

    // ── Flow ──────────────────────────────────────────────────────────────────

    override fun getPhotosFlow(tripId: String): Flow<List<TripPhoto>> =
        photosCol(tripId)
            .orderBy("uploadedAt")
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        TripPhoto(
                            id           = doc.id,
                            tripId       = tripId,
                            uploadedBy   = doc.get<String?>("uploadedBy")   ?: "",
                            uploaderName = doc.get<String?>("uploaderName") ?: "",
                            storageUrl   = doc.get<String?>("storageUrl")   ?: "",
                            // Firestore JS stores numbers as Double — convert back to Long
                            uploadedAt   = doc.get<Double?>("uploadedAt")?.toLong() ?: 0L,
                            caption      = doc.get<String?>("caption")      ?: "",
                        )
                    }.getOrNull()
                }
            }

    // ── Upload ────────────────────────────────────────────────────────────────

    override suspend fun uploadPhoto(
        tripId:     String,
        imageBytes: ByteArray,
        caption:    String,
    ): Result<TripPhoto> = runCatching {
        val user         = auth.currentUser ?: error("Not logged in")
        val photoId      = generateUniqueId()
        val uploaderName = PhotoBusinessRules.resolveUploaderName(user.displayName, user.email)

        // 1. Compress via canvas (scale to ≤ MAX_IMAGE_DIMENSION_PX, quality JPEG_QUALITY)
        val compressed: ByteArray = compressImage(imageBytes)

        // 2. Upload to Firebase Storage. ByteArray → Int8Array → Uint8Array
        // because Storage's Data wrapper expects an unsigned-byte view.
        val storageRef = storage.reference(PhotoBusinessRules.storagePathFor(tripId, photoId))
        val int8Array  = compressed.unsafeCast<Int8Array>()
        val uint8Array = Uint8Array(int8Array.buffer, int8Array.byteOffset, int8Array.length)
        storageRef.putData(Data(uint8Array), storageMetadata { contentType = "image/jpeg" })

        val downloadUrl = storageRef.getDownloadUrl()

        // 3. Save metadata to Firestore. `uploadedAt` must be Double (native JS number);
        // Kotlin Long is a two-Int struct in JS that the Firebase SDK rejects.
        val uploadedAtMs: Long = Date.now().toLong()
        val payload = PhotoBusinessRules.buildPhotoMetadataPayload(
            uploaderUid  = user.uid,
            uploaderName = uploaderName,
            downloadUrl  = downloadUrl,
            uploadedAtMs = uploadedAtMs,
            caption      = caption,
        ).toMutableMap().apply { put("uploadedAt", uploadedAtMs.toDouble()) }
        photosCol(tripId).document(photoId).set(payload)

        TripPhoto(
            id           = photoId,
            tripId       = tripId,
            uploadedBy   = user.uid,
            uploaderName = uploaderName,
            storageUrl   = downloadUrl,
            uploadedAt   = uploadedAtMs,
            caption      = caption,
        )
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    override suspend fun deletePhoto(
        tripId:     String,
        photoId:    String,
        storageUrl: String,
    ): Result<Unit> = runCatching {
        try {
            storage.reference(PhotoBusinessRules.storagePathFor(tripId, photoId)).delete()
        } catch (e: Exception) {
            // "storage/object-not-found" means it was already deleted — safe to ignore.
            // Any other error (network, permission, etc.) should propagate.
            val msg = e.message ?: ""
            if (!msg.contains("object-not-found") && !msg.contains("404")) throw e
        }

        photosCol(tripId).document(photoId).delete()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resize [bytes] so neither dimension exceeds [maxSizePx], then re-encode
     * as JPEG at [quality].  Falls back to the original bytes if any step fails.
     */
    private suspend fun compressImage(
        bytes:     ByteArray,
        maxSizePx: Int    = PhotoBusinessRules.MAX_IMAGE_DIMENSION_PX,
        // JS canvas API takes quality as 0.0-1.0, shared constant is 0-100 → divide.
        quality:   Double = PhotoBusinessRules.JPEG_QUALITY / 100.0,
    ): ByteArray = suspendCancellableCoroutine { cont ->
        try {
            // Wrap ByteArray in a JS Uint8Array, then Blob
            val jsUint8 = js("new Uint8Array(bytes.length)")
            for (i in bytes.indices) jsUint8[i] = bytes[i]
            val blob = Blob(arrayOf(jsUint8), BlobPropertyBag(type = "image/jpeg"))
            val blobUrl = URL.createObjectURL(blob)

            val img = Image()
            img.onerror = { _, _, _, _, _ ->
                URL.revokeObjectURL(blobUrl)
                cont.resume(bytes)
            }
            img.onload = { _ ->
                try {
                    val scale = minOf(
                        1.0,
                        minOf(
                            maxSizePx.toDouble() / img.naturalWidth,
                            maxSizePx.toDouble() / img.naturalHeight,
                        )
                    )
                    val w = (img.naturalWidth  * scale).toInt().coerceAtLeast(1)
                    val h = (img.naturalHeight * scale).toInt().coerceAtLeast(1)

                    val canvas = document.createElement("canvas") as HTMLCanvasElement
                    canvas.width  = w
                    canvas.height = h
                    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
                    ctx.drawImage(img, 0.0, 0.0, w.toDouble(), h.toDouble())

                    canvas.toBlob({ compBlob ->
                        URL.revokeObjectURL(blobUrl)
                        if (compBlob == null) { cont.resume(bytes); return@toBlob }
                        val reader = FileReader()
                        reader.onerror = { _ -> cont.resume(bytes) }
                        reader.onload  = { _ ->
                            val buf = reader.result as? ArrayBuffer
                            cont.resume(
                                if (buf != null) Int8Array(buf).unsafeCast<ByteArray>()
                                else             bytes
                            )
                        }
                        reader.readAsArrayBuffer(compBlob)
                    }, "image/jpeg", quality)
                } catch (_: Exception) {
                    URL.revokeObjectURL(blobUrl)
                    cont.resume(bytes)
                }
                null  // onload must return Unit/null
            }
            img.src = blobUrl
        } catch (_: Exception) {
            cont.resume(bytes)
        }
    }
}
