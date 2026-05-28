package com.domedemok.travelplanner.map

import kotlin.math.max
import kotlin.math.min

/**
 * Represents a geographic coordinate (latitude, longitude)
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        // Budapest default
        val BUDAPEST = LatLng(47.4979, 19.0402)
    }
}

/**
 * Represents the state of the map
 * @param center The center coordinate of the map
 * @param zoom Zoom level (5-18)
 * @param offsetX Horizontal offset in pixels (for panning)
 * @param offsetY Vertical offset in pixels (for panning)
 */
data class MapState(
    val center: LatLng,
    val zoom: Int,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    companion object {
        const val MIN_ZOOM = 5
        const val MAX_ZOOM = 18
        const val DEFAULT_ZOOM = 13
    }

    /**
     * Create a new state with zoom adjusted
     */
    fun withZoom(newZoom: Int): MapState {
        val clampedZoom = newZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        return copy(zoom = clampedZoom)
    }

    /**
     * Create a new state with offset adjusted (for panning)
     */
    fun withOffset(dx: Float, dy: Float): MapState {
        return copy(
            offsetX = offsetX + dx,
            offsetY = offsetY + dy
        )
    }

    /**
     * Zoom in by 1 level
     */
    fun zoomIn(): MapState = withZoom(zoom + 1)

    /**
     * Zoom out by 1 level
     */
    fun zoomOut(): MapState = withZoom(zoom - 1)
}

/**
 * Represents a tile coordinate in the tile grid
 * @param x Tile X coordinate
 * @param y Tile Y coordinate
 * @param z Zoom level
 */
data class TileCoordinate(
    val x: Int,
    val y: Int,
    val z: Int
) {
    /**
     * Get the URL for this tile from OpenStreetMap
     */
    fun toUrl(): String {
        // OpenStreetMap tile server
        // Round-robin across a, b, c subdomains for load balancing
        val subdomain = listOf("a", "b", "c")[x % 3]
        return "https://$subdomain.tile.openstreetmap.org/$z/$x/$y.png"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TileCoordinate) return false
        return x == other.x && y == other.y && z == other.z
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}