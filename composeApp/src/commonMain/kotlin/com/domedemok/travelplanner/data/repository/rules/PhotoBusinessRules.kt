package com.domedemok.travelplanner.data.repository.rules

/**
 * Pure decision logic for [com.domedemok.travelplanner.data.repository.PhotoRepository].
 *
 * Compression itself is unavoidably platform-specific (Android Bitmap vs.
 * Canvas/Blob in the browser), but the *parameters* of compression and the
 * storage path/payload shape can be shared.
 */
object PhotoBusinessRules {

    /**
     * Maximum dimension (px) after resizing. ≤ 1080 keeps photos sharp on
     * phones and laptops while typically yielding 100-200 KB JPEGs.
     */
    const val MAX_IMAGE_DIMENSION_PX: Int = 1080

    /** JPEG quality, 0–100. 75 is the sweet spot for photo content. */
    const val JPEG_QUALITY:           Int = 75

    /**
     * Storage object path for a trip photo: `trips/{tripId}/gallery/{photoId}.jpg`.
     * Centralised so a future rename only happens here.
     */
    fun storagePathFor(tripId: String, photoId: String): String =
        "trips/$tripId/gallery/$photoId.jpg"

    /**
     * Resolves the uploader's display name with the standard fallback chain:
     * Firebase `displayName` → email → empty string. Empty is a meaningful
     * sentinel (caller may decide to show "Anonymous").
     */
    fun resolveUploaderName(displayName: String?, email: String?): String =
        displayName?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: ""

    /**
     * Field-shape for `trips/{tripId}/photos/{photoId}` on insert.
     *
     * The platform impl is responsible for converting the [uploadedAt] Long
     * to a JS-friendly Double on the web target if needed.
     */
    fun buildPhotoMetadataPayload(
        uploaderUid:    String,
        uploaderName:   String,
        downloadUrl:    String,
        uploadedAtMs:   Long,
        caption:        String,
    ): Map<String, Any> = mapOf(
        "uploadedBy"   to uploaderUid,
        "uploaderName" to uploaderName,
        "storageUrl"   to downloadUrl,
        "uploadedAt"   to uploadedAtMs,
        "caption"      to caption,
    )
}
