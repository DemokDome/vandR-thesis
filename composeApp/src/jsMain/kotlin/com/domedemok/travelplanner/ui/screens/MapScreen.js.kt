package com.domedemok.travelplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.map.MapCanvas
import com.domedemok.travelplanner.map.MapState
import com.domedemok.travelplanner.map.LatLng
import com.domedemok.travelplanner.map.dayColor
import com.domedemok.travelplanner.ui.theme.*
import io.ktor.client.*
import io.ktor.client.engine.js.*

@Composable
actual fun MapScreen(
    places: List<Place>,
    itineraryDays: List<ItineraryDay>,
    tripId: String,
    currentUserId: String?,
    tripMembers: Map<String, String>,
    modifier: Modifier,
) {
    val s          = LocalStrings.current
    val httpClient = remember { HttpClient(Js) }

    if (places.isEmpty()) {
        EmptyMapState(modifier = modifier)
        return
    }

    // ── Day ↔ place look-up tables ────────────────────────────────────────────
    val placeById  = remember(places) { places.associateBy { it.id } }
    val sortedDays = remember(itineraryDays) { itineraryDays.sortedBy { it.dayNumber } }

    // placeId → 0-based day index (for colour coding)
    val placeToDay: Map<String, Int> = remember(sortedDays) {
        buildMap {
            sortedDays.forEachIndexed { idx, day ->
                day.placeIds.forEach { pid -> put(pid, idx) }
            }
        }
    }

    // ── Filter state ──────────────────────────────────────────────────────────
    var selectedDayId by remember { mutableStateOf<String?>(null) }
    var showSummary   by remember { mutableStateOf(false) }

    val visiblePlaces = remember(selectedDayId, sortedDays, places, placeById, placeToDay) {
        when (selectedDayId) {
            null              -> places
            "__unassigned__"  -> places.filter { it.id !in placeToDay }
            else              -> {
                val day = sortedDays.find { it.id == selectedDayId }
                day?.placeIds?.mapNotNull { placeById[it] } ?: emptyList()
            }
        }
    }

    // Re-center map when day selection changes
    val mapCenter = remember(selectedDayId, visiblePlaces) {
        visiblePlaces.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
            ?: places.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
            ?: LatLng.BUDAPEST
    }
    val initialState = remember(mapCenter) {
        MapState(center = mapCenter, zoom = MapState.DEFAULT_ZOOM)
    }

    Box(modifier = modifier) {

        // ── Map canvas ────────────────────────────────────────────────────────
        MapCanvas(
            places       = visiblePlaces,
            httpClient   = httpClient,
            initialState = initialState,
            placeToDay   = placeToDay,
            modifier     = Modifier.fillMaxSize(),
        )

        // ── Summary button (top-left; zoom controls are top-right) ──────────
        if (sortedDays.isNotEmpty()) {
            Surface(
                onClick       = { showSummary = !showSummary },
                modifier      = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 12.dp),
                shape         = AppShape.pill,
                color         = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector        = if (showSummary) Icons.Default.KeyboardArrowDown
                                            else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text       = s.mapSummaryButton,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // ── Day filter chips (bottom, above OSM attribution + zoom) ───────────
        if (sortedDays.isNotEmpty()) {
            val chipScrollState = rememberScrollState()
            val chipScope       = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 60.dp)
                    .horizontalScroll(chipScrollState)
                    // Mouse drag-to-scroll for the web: dragging right reveals earlier chips,
                    // dragging left reveals later ones (mirrors touch-swipe behaviour).
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            chipScope.launch { chipScrollState.scrollBy(-delta) }
                        },
                    )
                    .padding(horizontal = AppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                // "All" chip
                FilterChip(
                    selected = selectedDayId == null,
                    onClick  = { selectedDayId = null },
                    label    = { Text(s.mapAllDays, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor  = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor      = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )

                sortedDays.forEachIndexed { idx, day ->
                    val color     = dayColor(idx)
                    val isSelected = selectedDayId == day.id
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedDayId = if (isSelected) null else day.id },
                        label    = {
                            Text(
                                s.mapDayLabel(day.dayNumber),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.18f),
                            selectedLabelColor     = color,
                        ),
                    )
                }

                // "Unassigned" chip — only shown when there are unassigned places
                if (places.any { it.id !in placeToDay }) {
                    val isSelected = selectedDayId == "__unassigned__"
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedDayId = if (isSelected) null else "__unassigned__" },
                        label    = {
                            Text(
                                s.mapUnassigned,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }

        // ── Summary panel (slides up from bottom) ────────────────────────────
        AnimatedVisibility(
            visible = showSummary,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit  = slideOutVertically(targetOffsetY = { it }),
        ) {
            Surface(
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                shape         = AppShape.bottomSheet,
                color         = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppSpacing.lg)
                        .padding(top = AppSpacing.md, bottom = AppSpacing.xl)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Text(
                        text       = s.mapDailySummary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp,
                        modifier   = Modifier.padding(bottom = AppSpacing.xs),
                    )

                    if (sortedDays.isEmpty()) {
                        Text(
                            text  = s.mapNoDays,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        sortedDays.forEachIndexed { idx, day ->
                            val dayPlaces  = day.placeIds.mapNotNull { placeById[it] }
                            val color      = dayColor(idx)
                            val isSelected = selectedDayId == day.id

                            ElevatedCard(
                                shape    = AppShape.lg,
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                                onClick  = {
                                    selectedDayId = if (isSelected) null else day.id
                                    showSummary   = false
                                },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) color.copy(alpha = 0.1f)
                                            else Color.Transparent
                                        )
                                        .padding(AppSpacing.md),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                ) {
                                    // Coloured day dot
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = s.mapDayLabel(day.dayNumber),
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 14.sp,
                                        )
                                        if (dayPlaces.isEmpty()) {
                                            Text(
                                                text  = s.mapNoPlacesAssigned,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        } else {
                                            Text(
                                                text  = dayPlaces.joinToString(" · ") { it.name },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    // Place count badge
                                    if (dayPlaces.isNotEmpty()) {
                                        Surface(
                                            shape = AppShape.pill,
                                            color = color.copy(alpha = 0.15f),
                                        ) {
                                            Text(
                                                text     = "${dayPlaces.size}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color    = color,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Unassigned places
                        val assigned      = placeToDay.keys.toSet()
                        val unassigned    = places.filter { it.id !in assigned }
                        if (unassigned.isNotEmpty()) {
                            ElevatedCard(
                                shape    = AppShape.lg,
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                                onClick  = { selectedDayId = null; showSummary = false },
                            ) {
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(AppSpacing.md),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = s.mapUnassigned,
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 14.sp,
                                        )
                                        Text(
                                            text  = unassigned.joinToString(" · ") { it.name },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                    }
                                    Surface(
                                        shape = AppShape.pill,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Text(
                                            text     = "${unassigned.size}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMapState(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Box(
        modifier        = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier              = Modifier.padding(32.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "🗺️", style = MaterialTheme.typography.displayLarge)
            Text(text = s.mapNoPlacesTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                text  = s.mapNoPlacesBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
