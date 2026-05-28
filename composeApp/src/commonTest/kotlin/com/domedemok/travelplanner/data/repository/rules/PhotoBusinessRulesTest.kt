package com.domedemok.travelplanner.data.repository.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoBusinessRulesTest {

    // ── storagePathFor ──────────────────────────────────────────────────────

    @Test
    fun `storage path follows the trips gallery convention`() {
        assertEquals(
            "trips/abc/gallery/xyz.jpg",
            PhotoBusinessRules.storagePathFor(tripId = "abc", photoId = "xyz"),
        )
    }

    // ── resolveUploaderName ─────────────────────────────────────────────────

    @Test
    fun `uploader name prefers display name`() {
        assertEquals("Alice", PhotoBusinessRules.resolveUploaderName("Alice", "a@b.com"))
    }

    @Test
    fun `uploader name falls back to email when display name is blank`() {
        assertEquals("a@b.com", PhotoBusinessRules.resolveUploaderName("", "a@b.com"))
        assertEquals("a@b.com", PhotoBusinessRules.resolveUploaderName(null, "a@b.com"))
    }

    @Test
    fun `uploader name falls back to empty string when nothing is available`() {
        assertEquals("", PhotoBusinessRules.resolveUploaderName(null, null))
        assertEquals("", PhotoBusinessRules.resolveUploaderName("", ""))
    }

    // ── buildPhotoMetadataPayload ───────────────────────────────────────────

    @Test
    fun `metadata payload contains exactly the five expected fields`() {
        val payload = PhotoBusinessRules.buildPhotoMetadataPayload(
            uploaderUid  = "u-1",
            uploaderName = "Alice",
            downloadUrl  = "https://example.com/1.jpg",
            uploadedAtMs = 9_999L,
            caption      = "Sunset",
        )
        assertEquals(
            setOf("uploadedBy", "uploaderName", "storageUrl", "uploadedAt", "caption"),
            payload.keys,
        )
        assertEquals("u-1",                          payload["uploadedBy"])
        assertEquals("Alice",                        payload["uploaderName"])
        assertEquals("https://example.com/1.jpg",    payload["storageUrl"])
        assertEquals(9_999L,                         payload["uploadedAt"])
        assertEquals("Sunset",                       payload["caption"])
    }

    // ── Compression policy constants ────────────────────────────────────────

    @Test
    fun `compression policy is reasonable for photo content`() {
        assertTrue(PhotoBusinessRules.MAX_IMAGE_DIMENSION_PX > 0)
        assertTrue(PhotoBusinessRules.JPEG_QUALITY in 1..100)
    }
}
