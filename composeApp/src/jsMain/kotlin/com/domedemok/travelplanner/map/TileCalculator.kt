package com.domedemok.travelplanner.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.*

/**
 * Handles all mathematical calculations for tile-based maps
 * Uses Web Mercator projection (EPSG:3857)
 */
object TileCalculator {

    const val TILE_SIZE = 256 // Standard tile size in pixels

    /**
     * Convert latitude/longitude to tile coordinates at a given zoom level
     * Based on Web Mercator projection
     */
    fun latLngToTile(lat: Double, lng: Double, zoom: Int): TileCoordinate {
        val n = (1 shl zoom).toDouble()  // 2^zoom (bit shift for precision)

        // X: Longitude is linear
        val x = ((lng + 180.0) / 360.0 * n).toInt()

        // Y: Latitude uses Mercator projection formula
        val latRad = lat * PI / 180.0
        val y = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()

        return TileCoordinate(x, y, zoom)
    }

    /**
     * Convert latitude/longitude to pixel coordinates on the map
     * Uses Web Mercator projection (EPSG:3857)
     */
    fun latLngToPixels(
        latLng: LatLng,
        mapState: MapState,
        viewportSize: IntSize
    ): Offset {
        // Calculate world pixel coordinates at current zoom
        val scale = TILE_SIZE * (1 shl mapState.zoom)  // 2^zoom * 256

        // X: Simple linear mapping for longitude
        val worldX = (latLng.longitude + 180.0) / 360.0 * scale

        // Y: Mercator projection for latitude
        val latRad = latLng.latitude * PI / 180.0
        val worldY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale

        // Center world coordinates
        val centerWorldX = (mapState.center.longitude + 180.0) / 360.0 * scale
        val centerLatRad = mapState.center.latitude * PI / 180.0
        val centerWorldY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / PI) / 2.0 * scale

        // Convert to screen coordinates
        // The difference in world coords + viewport center + pan offset
        val screenX = (worldX - centerWorldX) + viewportSize.width / 2.0 + mapState.offsetX
        val screenY = (worldY - centerWorldY) + viewportSize.height / 2.0 + mapState.offsetY

        return Offset(screenX.toFloat(), screenY.toFloat())
    }

    /**
     * Get all visible tiles for the current viewport
     */
    fun getVisibleTiles(
        mapState: MapState,
        viewportSize: IntSize
    ): List<TileCoordinate> {
        val tiles = mutableListOf<TileCoordinate>()

        // Get center tile
        val centerTile = latLngToTile(
            mapState.center.latitude,
            mapState.center.longitude,
            mapState.zoom
        )

        // Calculate how many tiles we need in each direction
        // ⬅️ NÖVELVE: +4 extra tile mindkét irányban (2-2 buffer)
        val tilesWide = (viewportSize.width.toFloat() / TILE_SIZE).toInt() + 4
        val tilesHigh = (viewportSize.height.toFloat() / TILE_SIZE).toInt() + 4

        // Account for panning offset
        val offsetTilesX = (mapState.offsetX / TILE_SIZE).toInt()
        val offsetTilesY = (mapState.offsetY / TILE_SIZE).toInt()

        // Get all tiles in view
        val startX = centerTile.x - tilesWide / 2 - offsetTilesX
        val endX = centerTile.x + tilesWide / 2 - offsetTilesX
        val startY = centerTile.y - tilesHigh / 2 - offsetTilesY
        val endY = centerTile.y + tilesHigh / 2 - offsetTilesY

        val maxTile = (1 shl mapState.zoom) - 1  // 2^zoom - 1 (using bit shift)

        for (x in startX..endX) {
            for (y in startY..endY) {
                // Clamp Y (doesn't wrap)
                if (y in 0..maxTile) {
                    // Wrap X around (for crossing dateline)
                    val wrappedX = ((x % (maxTile + 1)) + (maxTile + 1)) % (maxTile + 1)
                    tiles.add(TileCoordinate(wrappedX, y, mapState.zoom))
                }
            }
        }

        return tiles
    }

    /**
     * Calculate the pixel position where a tile should be drawn
     * CRITICAL: Must use same coordinate system as latLngToPixels!
     */
    fun tileToPixelPosition(
        tile: TileCoordinate,
        mapState: MapState,
        viewportSize: IntSize
    ): Offset {
        // Calculate the TOP-LEFT corner of this tile in world coordinates
        // This must match the world coordinate system used in latLngToPixels
        val scale = TILE_SIZE * (1 shl mapState.zoom)  // 2^zoom * 256

        // Tile world coordinates (at the current zoom level)
        val tileWorldX = tile.x * TILE_SIZE.toDouble()
        val tileWorldY = tile.y * TILE_SIZE.toDouble()

        // Calculate center world coordinates (SAME as latLngToPixels)
        val centerWorldX = (mapState.center.longitude + 180.0) / 360.0 * scale
        val centerLatRad = mapState.center.latitude * PI / 180.0
        val centerWorldY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / PI) / 2.0 * scale

        // Convert to screen coordinates (SAME logic as latLngToPixels)
        val screenX = (tileWorldX - centerWorldX) + viewportSize.width / 2.0 + mapState.offsetX
        val screenY = (tileWorldY - centerWorldY) + viewportSize.height / 2.0 + mapState.offsetY

        return Offset(screenX.toFloat(), screenY.toFloat())
    }

    /**
     * Calculate the bounds (min/max lat/lng) for a list of coordinates
     */
    fun calculateBounds(coordinates: List<LatLng>): Pair<LatLng, LatLng>? {
        if (coordinates.isEmpty()) return null

        var minLat = coordinates[0].latitude
        var maxLat = coordinates[0].latitude
        var minLng = coordinates[0].longitude
        var maxLng = coordinates[0].longitude

        coordinates.forEach { coord ->
            minLat = min(minLat, coord.latitude)
            maxLat = max(maxLat, coord.latitude)
            minLng = min(minLng, coord.longitude)
            maxLng = max(maxLng, coord.longitude)
        }

        return Pair(
            LatLng(minLat, minLng),
            LatLng(maxLat, maxLng)
        )
    }

    /**
     * Convert pixel coordinates back to latitude/longitude
     * Used for zoom-to-point functionality
     */
    fun pixelsToLatLng(
        pixelPos: Offset,
        mapState: MapState,
        viewportSize: IntSize
    ): LatLng {
        val scale = TILE_SIZE * (1 shl mapState.zoom)  // 2^zoom * 256

        // Center world coordinates
        val centerWorldX = (mapState.center.longitude + 180.0) / 360.0 * scale
        val centerLatRad = mapState.center.latitude * PI / 180.0
        val centerWorldY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / PI) / 2.0 * scale

        // Convert screen coordinates to world coordinates
        val worldX = (pixelPos.x - viewportSize.width / 2.0 - mapState.offsetX) + centerWorldX
        val worldY = (pixelPos.y - viewportSize.height / 2.0 - mapState.offsetY) + centerWorldY

        // Convert world coordinates to lat/lng
        val longitude = (worldX / scale) * 360.0 - 180.0

        // Inverse Mercator for latitude
        val n = PI - (2.0 * PI * worldY / scale)
        val latitude = atan(sinh(n)) * 180.0 / PI

        return LatLng(latitude, longitude)
    }

    /**
     * Calculate new center for zoom operation to keep a specific pixel position fixed
     * @param pixelPos The pixel position to keep fixed (usually mouse position)
     * @param pointLatLng The lat/lng at that pixel position in current zoom
     * @param newZoom Target zoom level
     * @param viewportSize Viewport dimensions
     * @return New center lat/lng for the target zoom level
     */
    fun calculateNewCenterForZoom(
        pixelPos: Offset,
        pointLatLng: LatLng,
        newZoom: Int,
        viewportSize: IntSize
    ): LatLng {
        // Calculate world coordinates of the fixed point at NEW zoom level
        val newScale = TILE_SIZE * (1 shl newZoom)
        val pointWorldX = (pointLatLng.longitude + 180.0) / 360.0 * newScale
        val pointLatRad = pointLatLng.latitude * PI / 180.0
        val pointWorldY = (1.0 - ln(tan(pointLatRad) + 1.0 / cos(pointLatRad)) / PI) / 2.0 * newScale

        // We want: pixelPos = (pointWorld - centerWorld) + viewport/2
        // So: centerWorld = pointWorld - (pixelPos - viewport/2)
        val centerWorldX = pointWorldX - (pixelPos.x - viewportSize.width / 2.0)
        val centerWorldY = pointWorldY - (pixelPos.y - viewportSize.height / 2.0)

        // Convert center world coordinates back to lat/lng
        val centerLng = (centerWorldX / newScale) * 360.0 - 180.0
        val n = PI - (2.0 * PI * centerWorldY / newScale)
        val centerLat = atan(sinh(n)) * 180.0 / PI

        return LatLng(centerLat, centerLng)
    }
}