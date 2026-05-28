package com.domedemok.travelplanner.data.repository.rules

import com.domedemok.travelplanner.data.model.Place
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaceBusinessRulesTest {

    private val samplePlace = Place(
        id             = "p-1",
        tripId         = "t-1",
        name           = "Eiffel Tower",
        address        = "Paris",
        latitude       = 48.85,
        longitude      = 2.29,
        notes          = "Visit at sunset",
        category       = "Landmark",
        foursquareId   = "fsq-123",
        addedAt        = 1_700_000_000_000L,
        addedBy        = "u-1",
        rating         = 9.3,
        popularity     = 0.95,
        photoUrl       = "https://example.com/eiffel.jpg",
        distanceMeters = 4321, // ephemeral, should NOT appear in any payload
    )

    // ── Add payload ─────────────────────────────────────────────────────────

    @Test
    fun `add payload includes every persisted field`() {
        val payload = PlaceBusinessRules.buildAddPayload(samplePlace, addedByUid = "u-5")
        assertEquals("Eiffel Tower",                 payload["name"])
        assertEquals("Paris",                        payload["address"])
        assertEquals(48.85,                          payload["latitude"])
        assertEquals(2.29,                           payload["longitude"])
        assertEquals("Visit at sunset",              payload["notes"])
        assertEquals("Landmark",                     payload["category"])
        assertEquals("fsq-123",                      payload["foursquareId"])
        assertEquals(1_700_000_000_000L,             payload["addedAt"])
        assertEquals("u-5",                          payload["addedBy"])
        assertEquals(9.3,                            payload["rating"])
        assertEquals(0.95,                           payload["popularity"])
        assertEquals("https://example.com/eiffel.jpg", payload["photoUrl"])
    }

    @Test
    fun `add payload omits ephemeral distanceMeters even when non-zero`() {
        val payload = PlaceBusinessRules.buildAddPayload(samplePlace, addedByUid = "u-5")
        assertFalse("distanceMeters" in payload.keys)
    }

    @Test
    fun `add payload uses caller-supplied addedByUid not the place model field`() {
        val payload = PlaceBusinessRules.buildAddPayload(samplePlace, addedByUid = "different-uid")
        assertEquals("different-uid", payload["addedBy"])
    }

    // ── Update payload ──────────────────────────────────────────────────────

    @Test
    fun `update payload includes only user-editable fields`() {
        val payload = PlaceBusinessRules.buildUpdatePayload(samplePlace)
        assertEquals(
            setOf("name", "address", "latitude", "longitude", "notes", "category"),
            payload.keys,
        )
    }

    @Test
    fun `update payload excludes addedAt to preserve sort order`() {
        val payload = PlaceBusinessRules.buildUpdatePayload(samplePlace)
        assertFalse("addedAt" in payload.keys)
    }

    @Test
    fun `update payload excludes provenance and enrichment fields`() {
        val payload = PlaceBusinessRules.buildUpdatePayload(samplePlace)
        assertFalse("addedBy" in payload.keys)
        assertFalse("foursquareId" in payload.keys)
        assertFalse("rating" in payload.keys)
        assertFalse("popularity" in payload.keys)
        assertFalse("photoUrl" in payload.keys)
    }

    @Test
    fun `update payload values mirror the input place`() {
        val payload = PlaceBusinessRules.buildUpdatePayload(samplePlace)
        assertEquals("Eiffel Tower", payload["name"])
        assertTrue(payload["latitude"] == 48.85)
    }
}
