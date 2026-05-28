package com.domedemok.travelplanner.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.ui.components.EmojiText
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.ItineraryViewModel
import com.domedemok.travelplanner.viewmodel.PlaceViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ── Drag state holder (shared across the whole screen) ────────────────────────

private class DragDropState {
    var activeDayId  by mutableStateOf<String?>(null)
    var activeIndex  by mutableStateOf<Int?>(null)
    var offsetY      by mutableStateOf(0f)
    var itemHeight   by mutableStateOf(72f) // default estimate
    var isDragging   by mutableStateOf(false)

    fun startDrag(dayId: String, index: Int, measuredHeight: Float) {
        activeDayId = dayId
        activeIndex = index
        itemHeight  = measuredHeight.coerceAtLeast(1f)
        offsetY     = 0f
        isDragging  = true
    }

    /** Returns non-null step count when a swap threshold is crossed. */
    fun applyDelta(delta: Float): Int? {
        offsetY += delta
        val steps = (offsetY / itemHeight).roundToInt()
        return if (steps != 0) { offsetY -= steps * itemHeight; steps } else null
    }

    fun reset() {
        activeDayId = null; activeIndex = null; offsetY = 0f; isDragging = false
    }
}

// ── Entry point ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ItineraryScreen(
    tripId: String,
    trip: Trip?,
    modifier: Modifier = Modifier,
    viewModel: ItineraryViewModel = koinViewModel(),
    placeViewModel: PlaceViewModel = koinViewModel(),
) {
    val uiState       by viewModel.uiState.collectAsState()
    val placeUiState  by placeViewModel.uiState.collectAsState()

    // Keep the itinerary VM in sync with the saved-places list.
    LaunchedEffect(placeUiState.places) {
        viewModel.updateSavedPlaces(placeUiState.places)
    }

    // Bootstrap day documents whenever the trip data arrives.
    LaunchedEffect(trip?.id, trip?.startDate, trip?.endDate) {
        if (trip != null) {
            viewModel.load(
                tripId       = tripId,
                startDate    = trip.startDate,
                endDate      = trip.endDate,
                savedPlaces  = placeUiState.places,
            )
        }
    }

    // Places that have not been assigned to any day yet.
    val assignedIds    = remember(uiState.days) { uiState.days.flatMap { it.placeIds }.toSet() }
    val unassignedPlaces = remember(uiState.savedPlaces, assignedIds) {
        uiState.savedPlaces.filter { it.id !in assignedIds }
    }

    val dragState        = remember { DragDropState() }
    val listState        = rememberLazyListState()
    var addingToDayId    by remember { mutableStateOf<String?>(null) }
    var dayPickerPlace   by remember { mutableStateOf<Place?>(null) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Show the placeholder whenever EITHER date is missing — the
            // itinerary needs a closed [start, end] range to materialise day
            // slots, and ItineraryViewModel.load skips ensureDaysExist
            // otherwise (which would leave the screen in an infinite spinner).
            trip != null && (trip.startDate == 0L || trip.endDate == 0L) -> {
                NoDatesEmptyState()
            }

            uiState.days.isEmpty() && !uiState.isLoading -> {
                // Dates are set but day docs haven't arrived yet (first write / cold start).
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text  = "Setting up your itinerary…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    state           = listState,
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(
                        start  = AppSpacing.lg,
                        end    = AppSpacing.lg,
                        top    = AppSpacing.md,
                        bottom = AppSpacing.xxxl + AppSpacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    // ── Day cards ─────────────────────────────────────────────
                    items(items = uiState.days, key = { it.id }) { day ->
                        val dayPlaces = remember(day.placeIds, uiState.savedPlaces) {
                            day.placeIds.mapNotNull { id ->
                                uiState.savedPlaces.find { it.id == id }
                            }
                        }
                        DayCard(
                            day          = day,
                            places       = dayPlaces,
                            dragState    = dragState,
                            onAddClick   = { addingToDayId = day.id },
                            onRemove     = { placeId ->
                                viewModel.removePlaceFromDay(tripId, day.id, placeId)
                            },
                            onReorder    = { from, to ->
                                viewModel.reorderInDay(tripId, day.id, from, to)
                            },
                        )
                    }

                    // ── Unassigned pool ───────────────────────────────────────
                    if (unassignedPlaces.isNotEmpty()) {
                        item {
                            UnassignedSection(
                                places    = unassignedPlaces,
                                days      = uiState.days,
                                onPickDay = { place -> dayPickerPlace = place },
                            )
                        }
                    }
                }
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AppSpacing.lg),
                action   = { TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") } },
            ) { Text(error) }
        }
    }

    // ── Add-place bottom sheet (opened via day card + button) ────────────────
    val dayId = addingToDayId
    if (dayId != null) {
        AddPlaceSheet(
            unassignedPlaces  = unassignedPlaces,
            totalSavedCount   = uiState.savedPlaces.size,
            onAdd = { placeId ->
                viewModel.addPlaceToDay(tripId, dayId, placeId)
                addingToDayId = null
            },
            onDismiss = { addingToDayId = null },
        )
    }

    // ── Day-picker sheet (opened by tapping a place in the unassigned section)
    val pickerPlace = dayPickerPlace
    if (pickerPlace != null) {
        DayPickerSheet(
            place     = pickerPlace,
            days      = uiState.days,
            onPick    = { dayId2 ->
                viewModel.addPlaceToDay(tripId, dayId2, pickerPlace.id)
                dayPickerPlace = null
            },
            onDismiss = { dayPickerPlace = null },
        )
    }
}

// ── Day card ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
private fun DayCard(
    day: ItineraryDay,
    places: List<Place>,
    dragState: DragDropState,
    onAddClick: () -> Unit,
    onRemove: (placeId: String) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.md)) {

            // ── Day header ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = "${day.dayNumber}",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column {
                        Text(
                            text       = "Day ${day.dayNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                        if (day.date > 0L) {
                            Text(
                                text     = formatDayDate(day.date, com.domedemok.travelplanner.i18n.LocalStrings.current),
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Place count badge + add button
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    if (places.isNotEmpty()) {
                        Surface(
                            shape = AppShape.pill,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text     = "${places.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    FilledIconButton(
                        onClick  = onAddClick,
                        modifier = Modifier.size(32.dp),
                        shape    = CircleShape,
                        colors   = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add place", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Divider + drag hint ────────────────────────────────────────
            if (places.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                    color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                if (places.size > 1) {
                    Text(
                        text      = "Hold ≡ to reorder",
                        fontSize  = 10.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.End,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppSpacing.xs),
                    )
                }
            }

            // ── Draggable place rows ────────────────────────────────────────
            places.forEachIndexed { index, place ->
                val isDraggingThis = dragState.isDragging &&
                        dragState.activeDayId == day.id &&
                        dragState.activeIndex == index

                val elevation by animateDpAsState(
                    targetValue   = if (isDraggingThis) 8.dp else 0.dp,
                    animationSpec = tween(150),
                    label         = "dragElevation",
                )

                DraggableItineraryItem(
                    place           = place,
                    isDragging      = isDraggingThis,
                    elevation       = elevation,
                    onRemove        = { onRemove(place.id) },
                    onDragStart     = { height ->
                        dragState.startDrag(day.id, index, height)
                    },
                    onDrag          = { delta ->
                        val steps = dragState.applyDelta(delta) ?: return@DraggableItineraryItem
                        val idx   = dragState.activeIndex ?: return@DraggableItineraryItem
                        val newIdx = (idx + steps).coerceIn(0, places.lastIndex)
                        if (newIdx != idx) {
                            onReorder(idx, newIdx)
                            dragState.activeIndex = newIdx
                        }
                    },
                    onDragEnd       = { dragState.reset() },
                )
            }

            // ── Empty state inside the card ─────────────────────────────────
            if (places.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text      = "Tap + to add places for this day",
                        fontSize  = 13.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ── Draggable item row ────────────────────────────────────────────────────────

@Composable
private fun DraggableItineraryItem(
    place: Place,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onRemove: () -> Unit,
    onDragStart: (itemHeightPx: Float) -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    var itemHeightPx by remember { mutableStateOf(0f) }
    val visual = CategoryCatalog.forLabel(place.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation = elevation, shape = AppShape.md)
            .background(
                color = if (isDragging)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else Color.Transparent,
                shape = AppShape.md,
            )
            .padding(vertical = 4.dp)
            .onGloballyPositioned { itemHeightPx = it.size.height.toFloat() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        // Drag handle
        Icon(
            imageVector        = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier           = Modifier
                .size(24.dp)
                .pointerInput(place.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart(itemHeightPx) },
                        onDrag      = { _, dragAmount -> onDrag(dragAmount.y) },
                        onDragEnd   = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                    )
                },
        )

        // Category emoji
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppShape.md)
                .background(visual.tintBg),
            contentAlignment = Alignment.Center,
        ) {
            EmojiText(visual.emoji, fontSize = 18.sp)
        }

        // Name + category
        Column(modifier = Modifier.weight(1f)) {
            EmojiText(
                text       = place.name,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = visual.label,
                fontSize = 11.sp,
                color    = visual.accent,
            )
        }

        // Remove button
        IconButton(
            onClick  = onRemove,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.RemoveCircleOutline,
                contentDescription = "Remove from day",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}

// ── Unassigned pool section ───────────────────────────────────────────────────

@Composable
private fun UnassignedSection(
    places:    List<Place>,
    days:      List<ItineraryDay>,
    onPickDay: (Place) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Text(
                text          = "NOT YET PLANNED",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Black,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
            )
            Surface(
                shape = AppShape.pill,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text       = "${places.size}",
                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text     = "Tap a place to assign it to a day",
                fontSize = 10.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }

        places.forEach { place ->
            val visual = CategoryCatalog.forLabel(place.category)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShape.lg)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { if (days.isNotEmpty()) onPickDay(place) }
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(AppShape.md)
                        .background(visual.tintBg),
                    contentAlignment = Alignment.Center,
                ) {
                    EmojiText(visual.emoji, fontSize = 16.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EmojiText(
                        text       = place.name,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Text(
                        text     = visual.label,
                        fontSize = 11.sp,
                        color    = visual.accent,
                    )
                }
                // "Plan" action chip
                Surface(
                    shape    = AppShape.pill,
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    onClick  = { if (days.isNotEmpty()) onPickDay(place) },
                    modifier = Modifier,
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = AppSpacing.sm + 2.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Add,
                            contentDescription = null,
                            modifier           = Modifier.size(11.dp),
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text       = "Plan",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// ── Add-place bottom sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlaceSheet(
    unassignedPlaces: List<Place>,
    totalSavedCount:  Int,
    onAdd:            (placeId: String) -> Unit,
    onDismiss:        () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = AppShape.bottomSheet,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        PopupThemeProvider {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Text(
                text       = "Add a place",
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            if (unassignedPlaces.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier           = Modifier.size(48.dp),
                            tint               = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (totalSavedCount == 0)
                                "No places saved yet.\nGo to the Places tab to discover and save spots first."
                            else
                                "All saved places are already planned!",
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    items(items = unassignedPlaces, key = { it.id }) { place ->
                        val visual = CategoryCatalog.forLabel(place.category)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShape.lg)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(AppSpacing.md),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(AppShape.md)
                                    .background(visual.tintBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                EmojiText(visual.emoji, fontSize = 20.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                EmojiText(
                                    text       = place.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 14.sp,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text     = visual.label,
                                    fontSize = 11.sp,
                                    color    = visual.accent,
                                )
                            }
                            FilledTonalButton(
                                onClick      = { onAdd(place.id) },
                                shape        = AppShape.pill,
                                contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = 0.dp),
                                modifier     = Modifier.height(32.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

// ── Day-picker sheet ──────────────────────────────────────────────────────────
// Shown when the user taps "Plan" on an unassigned place.  Lists every day so
// the user can pick which day to assign the place to.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun DayPickerSheet(
    place:     Place,
    days:      List<ItineraryDay>,
    onPick:    (dayId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val visual     = CategoryCatalog.forLabel(place.category)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = AppShape.bottomSheet,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        PopupThemeProvider {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            // Which place we're assigning
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppShape.md)
                        .background(visual.tintBg),
                    contentAlignment = Alignment.Center,
                ) { EmojiText(visual.emoji, fontSize = 20.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "Add to which day?",
                        fontSize   = 12.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    EmojiText(
                        text       = place.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Day list
            days.forEach { day ->
                Surface(
                    shape    = AppShape.lg,
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick  = { onPick(day.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier              = Modifier.padding(AppSpacing.md),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = "${day.dayNumber}",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = "Day ${day.dayNumber}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp,
                            )
                            if (day.date > 0L) {
                                Text(
                                    text     = formatDayDate(day.date, com.domedemok.travelplanner.i18n.LocalStrings.current),
                                    fontSize = 12.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // How many places are already on this day
                        if (day.placeIds.isNotEmpty()) {
                            Surface(
                                shape = AppShape.pill,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text     = "${day.placeIds.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        Icon(
                            imageVector        = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        }
    }
}

// ── No-dates empty state ──────────────────────────────────────────────────────

@Composable
private fun NoDatesEmptyState() {
    Column(
        modifier            = Modifier.fillMaxSize().padding(AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape    = CircleShape,
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier           = Modifier.size(40.dp),
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.lg))
        Text(
            text       = "No dates set",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Text(
            text      = "Set start and end dates for this trip to\nbuild a day-by-day itinerary.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Date helpers ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
private fun formatDayDate(epochMs: Long, strings: com.domedemok.travelplanner.i18n.Strings): String {
    if (epochMs == 0L) return ""
    val validMs  = if (epochMs < 1_000_000_000_000L) epochMs * 1000 else epochMs
    val instant  = Instant.fromEpochMilliseconds(validMs)
    val dt       = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    // newTripWeekDayLabels is Sunday-first by contract — reuse it directly so
    // the locale's two-letter weekday abbreviations match across screens.
    val dow      = dt.dayOfWeek.ordinal  // Mon=0 in kotlinx, shift for Sun=0
    val sunFirst = (dow + 1) % 7
    return "${strings.newTripWeekDayLabels[sunFirst]}, ${strings.monthShortNames[dt.month.ordinal]} ${dt.day}"
}
