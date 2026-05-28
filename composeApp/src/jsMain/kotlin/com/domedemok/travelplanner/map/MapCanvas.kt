package com.domedemok.travelplanner.map

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Place
import io.ktor.client.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged

/**
 * Main map canvas component that renders the tile-based map
 */
/** Day colour palette — mirrors the Android implementation. */
val dayColors = listOf(
    Color(0xFF0EA5E9), // 1 – sky-blue
    Color(0xFF22C55E), // 2 – green
    Color(0xFFF97316), // 3 – orange
    Color(0xFF8B5CF6), // 4 – violet
    Color(0xFFEC4899), // 5 – pink
    Color(0xFF06B6D4), // 6 – cyan
    Color(0xFFEAB308), // 7 – yellow
    Color(0xFFD946EF), // 8 – fuchsia
    Color(0xFF3B82F6), // 9 – blue
    Color(0xFFEF4444), // 10 – red
)
fun dayColor(index: Int): Color = dayColors[index % dayColors.size]

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapCanvas(
    places: List<Place>,
    httpClient: HttpClient,
    modifier: Modifier = Modifier,
    initialState: MapState = MapState(
        center = if (places.isNotEmpty())
            LatLng(places[0].latitude, places[0].longitude)
        else
            LatLng.BUDAPEST,
        zoom = MapState.DEFAULT_ZOOM
    ),
    /** placeId → 0-based day index; empty = no itinerary assigned */
    placeToDay: Map<String, Int> = emptyMap(),
) {
    var mapState by remember { mutableStateOf(initialState) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }

    // React to programmatic recenter requests from the parent. Without this,
    // the `remember { initialState }` above would freeze the camera at the
    // value from first composition — so when MapScreen recomputes a new
    // `initialState` (e.g. the user picked a different itinerary day), the
    // map would stay stuck on the original day's first place.
    //
    // Pan/zoom offsets are cleared too because they're now meaningless against
    // a new center.
    LaunchedEffect(initialState.center, initialState.zoom) {
        mapState = mapState.copy(
            center  = initialState.center,
            zoom    = initialState.zoom,
            offsetX = 0f,
            offsetY = 0f,
        )
        selectedPlace = null
    }

    val tileLoader = remember(httpClient) { TileLoader(httpClient) }
    val textMeasurer = rememberTextMeasurer()
    val markerRenderer = remember { MarkerRenderer(textMeasurer) }

    // ⬅️ ÚJ: Track loaded tiles to trigger recomposition
    val loadedTiles = remember { mutableStateOf(0) }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // ⬅️ ÚJ: Force initial render and preload
    LaunchedEffect(mapState.zoom) {
        kotlinx.coroutines.delay(50)
        loadedTiles.value++
    }

    DisposableEffect(Unit) {
        onDispose {
            tileLoader.dispose()
        }
    }

    Box(modifier = modifier
        .clipToBounds()
        .onSizeChanged { viewportSize = it }
    ) {
        // Main map canvas
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    // Mouse wheel zoom
                    val scrollDelta = event.changes.first().scrollDelta.y
                    val viewportSize = IntSize(size.width, size.height)
                    val pointerPosition = event.changes.first().position

                    // Calculate new zoom level
                    val zoomDelta = if (scrollDelta > 0) -1 else 1
                    val newZoom = (mapState.zoom + zoomDelta).coerceIn(MapState.MIN_ZOOM, MapState.MAX_ZOOM)

                    if (newZoom != mapState.zoom) {
                        // Get lat/lng at pointer position with CURRENT zoom
                        val pointLatLng = TileCalculator.pixelsToLatLng(
                            pointerPosition,
                            mapState,
                            viewportSize
                        )

                        // Calculate new center so that pointLatLng stays at pointerPosition
                        val newCenter = TileCalculator.calculateNewCenterForZoom(
                            pointerPosition,
                            pointLatLng,
                            newZoom,
                            viewportSize
                        )

                        // Update state with new zoom and center
                        mapState = mapState.copy(
                            zoom = newZoom,
                            center = newCenter,
                            offsetX = 0f,
                            offsetY = 0f
                        )
                        selectedPlace = null
                        loadedTiles.value++
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        mapState = mapState.withOffset(dragAmount.x, dragAmount.y)
                        selectedPlace = null // Close popup on drag
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Calculate marker positions
                        val markerPositions = mutableMapOf<String, Offset>()
                        places.forEach { place ->
                            val pos = TileCalculator.latLngToPixels(
                                LatLng(place.latitude, place.longitude),
                                mapState,
                                IntSize(size.width, size.height)
                            )
                            markerPositions[place.id] = pos
                        }

                        // Check if a marker was clicked
                        val clickedPlace = markerRenderer.findClickedPlace(
                            offset,
                            places,
                            markerPositions
                        )

                        selectedPlace = if (clickedPlace == selectedPlace) {
                            null // Toggle off if same marker clicked
                        } else {
                            clickedPlace
                        }
                    }
                }
        ) {
            val viewportSize = IntSize(size.width.toInt(), size.height.toInt())

            // Force read state to ensure recomposition
            loadedTiles.value

            // 1. Draw map tiles
            drawMapTiles(mapState, viewportSize, tileLoader) {
                loadedTiles.value++  // ⬅️ JAVÍTVA
            }

            // 2. Draw markers (colour-coded by itinerary day when available)
            val markerPositions = mutableMapOf<String, Offset>()
            places.forEach { place ->
                val position = TileCalculator.latLngToPixels(
                    LatLng(place.latitude, place.longitude),
                    mapState,
                    viewportSize
                )
                markerPositions[place.id] = position

                val tint = placeToDay[place.id]?.let { dayColor(it) } ?: Color(0xFFEF5350)
                with(markerRenderer) {
                    drawMarker(position, isSelected = place == selectedPlace, tint = tint)
                }
            }

            // 3. Draw popup for selected place
            selectedPlace?.let { place ->
                markerPositions[place.id]?.let { position ->
                    with(markerRenderer) {
                        drawPopup(place, position)
                    }
                }
            }
        }

        // Zoom controls — top-right so they never overlap the day-filter chip row
        ZoomControls(
            mapState = mapState,
            onZoomIn = {
                if (viewportSize != IntSize.Zero) {
                    val centerPixel = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val trueCenter  = TileCalculator.pixelsToLatLng(centerPixel, mapState, viewportSize)
                    mapState = mapState.copy(
                        center  = trueCenter,
                        zoom    = (mapState.zoom + 1).coerceAtMost(MapState.MAX_ZOOM),
                        offsetX = 0f,
                        offsetY = 0f,
                    )
                    selectedPlace = null
                    loadedTiles.value++
                }
            },
            onZoomOut = {
                if (viewportSize != IntSize.Zero) {
                    val centerPixel = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val trueCenter  = TileCalculator.pixelsToLatLng(centerPixel, mapState, viewportSize)
                    mapState = mapState.copy(
                        center  = trueCenter,
                        zoom    = (mapState.zoom - 1).coerceAtLeast(MapState.MIN_ZOOM),
                        offsetX = 0f,
                        offsetY = 0f,
                    )
                    selectedPlace = null
                    loadedTiles.value++
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
        )

        // Attribution (required by OpenStreetMap license - DO NOT REMOVE!)
        // Bal alsó sarok - nem ütközik a zoom controls-szal
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)  // ⬅️ VÁLTOZTATVA: BottomEnd → BottomStart
                .padding(4.dp),
            color = Color.White.copy(alpha = 0.7f),  // ⬅️ Átlátszóbb
            shape = MaterialTheme.shapes.extraSmall,
            shadowElevation = 1.dp
        ) {
            Text(
                text = "© OpenStreetMap",  // ⬅️ Rövidebb
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp  // ⬅️ Kisebb
                ),
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Draw all visible map tiles
 */
private fun DrawScope.drawMapTiles(
    mapState: MapState,
    viewportSize: IntSize,
    tileLoader: TileLoader,
    onTileLoaded: () -> Unit
) {
    // Get visible tiles
    val visibleTiles = TileCalculator.getVisibleTiles(mapState, viewportSize)

    // Draw each tile
    visibleTiles.forEach { tile ->
        val position = TileCalculator.tileToPixelPosition(tile, mapState, viewportSize)

        // Try to get from cache
        val image = tileLoader.getFromCache(tile)

        if (image != null) {
            // Draw the tile
            drawImage(
                image = image,
                topLeft = position,
                alpha = 1f
            )
        } else {
            // Draw placeholder while loading
            drawRect(
                color = Color(0xFFF5F5F5),
                topLeft = position,
                size = Size(256f, 256f)
            )

            // Start loading
            tileLoader.loadTile(tile) {
                onTileLoaded()
            }
        }
    }
}

/**
 * Zoom control buttons (+/-)
 */
@Composable
private fun ZoomControls(
    mapState: MapState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),  // ⬅️ Csökkentve 8dp → 4dp
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom in button
        FloatingActionButton(
            onClick = onZoomIn,
            modifier = Modifier.size(40.dp),  // ⬅️ Csökkentve 48dp → 40dp
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp  // ⬅️ Finomabb árnyék
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom in",
                modifier = Modifier.size(20.dp)  // ⬅️ Kisebb ikon
            )
        }

        // ⬅️ ZOOM SZÁM ELTÁVOLÍTVA - Nincs Surface/Text közötte

        // Zoom out button
        FloatingActionButton(
            onClick = onZoomOut,
            modifier = Modifier.size(40.dp),  // ⬅️ Csökkentve 48dp → 40dp
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp  // ⬅️ Finomabb árnyék
            )
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom out",
                modifier = Modifier.size(20.dp)  // ⬅️ Kisebb ikon
            )
        }
    }
}