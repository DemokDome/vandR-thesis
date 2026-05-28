package com.domedemok.travelplanner.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.*
import org.jetbrains.skia.Image

/**
 * Handles asynchronous loading and caching of map tiles using Ktor HttpClient
 */
class TileLoader(
    private val httpClient: HttpClient
) {

    // LRU Cache for loaded tiles
    private val cache = mutableMapOf<TileCoordinate, ImageBitmap>()
    private val loading = mutableSetOf<TileCoordinate>()

    // Cache configuration
    private val maxCacheSize = 200 // Maximum tiles to keep in memory
    private val accessOrder = mutableListOf<TileCoordinate>() // For LRU

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Load a tile image, using cache if available
     */
    fun loadTile(
        tile: TileCoordinate,
        onLoaded: (ImageBitmap) -> Unit
    ) {
        // Check cache first
        cache[tile]?.let {
            updateAccessOrder(tile)
            onLoaded(it)
            return
        }

        // Already loading?
        if (loading.contains(tile)) return

        // Start loading
        loading.add(tile)

        scope.launch {
            try {
                val image = downloadTile(tile)

                // Add to cache
                cache[tile] = image
                accessOrder.add(tile)

                // Evict old tiles if cache is full
                if (cache.size > maxCacheSize) {
                    evictOldestTile()
                }

                onLoaded(image)
            } catch (e: Exception) {
                console.error("Failed to load tile $tile:", e)
            } finally {
                loading.remove(tile)
            }
        }
    }

    /**
     * Check if a tile is in cache
     */
    fun isInCache(tile: TileCoordinate): Boolean {
        return cache.containsKey(tile)
    }

    /**
     * Get a tile from cache (if available)
     */
    fun getFromCache(tile: TileCoordinate): ImageBitmap? {
        return cache[tile]?.also {
            updateAccessOrder(tile)
        }
    }

    /**
     * Download a single tile image from the server using Ktor HttpClient
     */
    private suspend fun downloadTile(tile: TileCoordinate): ImageBitmap {
        val url = tile.toUrl()

        // Use Ktor to download the tile
        val response: HttpResponse = httpClient.get(url)
        val bytes: ByteArray = response.readRawBytes()

        // Decode image using Skia
        val skiaImage = Image.makeFromEncoded(bytes)
        return skiaImage.toComposeImageBitmap()
    }

    /**
     * Update the access order for LRU eviction
     */
    private fun updateAccessOrder(tile: TileCoordinate) {
        accessOrder.remove(tile)
        accessOrder.add(tile)
    }

    /**
     * Evict the least recently used tile from cache
     */
    private fun evictOldestTile() {
        if (accessOrder.isNotEmpty()) {
            val oldest = accessOrder.removeAt(0)
            cache.remove(oldest)
            console.log("Evicted tile from cache: $oldest")
        }
    }

    /**
     * Clear all cached tiles
     */
    fun clearCache() {
        cache.clear()
        accessOrder.clear()
        console.log("Tile cache cleared")
    }

    /**
     * Preload tiles around the center for smoother experience
     */
    fun preloadTiles(tiles: List<TileCoordinate>) {
        tiles.forEach { tile ->
            if (!isInCache(tile) && !loading.contains(tile)) {
                loadTile(tile) { /* Preload, no callback needed */ }
            }
        }
    }

    /**
     * Cancel all pending loads and clear resources
     */
    fun dispose() {
        scope.cancel()
        clearCache()
    }
}