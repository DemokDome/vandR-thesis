package com.domedemok.travelplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.data.remote.FoursquareCategories
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.components.EmojiText
import com.domedemok.travelplanner.ui.components.LocalIsOnline
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.PlaceViewModel
import org.koin.compose.viewmodel.koinViewModel

// ── Category palette ────────────────────────────────────────────────────────
// Mirrors the Figma colour assignments: each travel category has a background
// tint (for the 44dp icon box) and an accent colour (badge text / filter pill).

data class CategoryVisual(
    val id:       String,
    val label:    String,
    val emoji:    String,
    val tintBg:   Color,   // soft background for the icon chip
    val accent:   Color,   // text / border colour
)

object CategoryCatalog {
    val Sights        = CategoryVisual("sights",        "Sights",        "🏛️", Color(0xFFECEAFD), Color(0xFF6D5FE8))
    val Museums       = CategoryVisual("museums",       "Museums",       "🖼️", Color(0xFFEDE9FE), Color(0xFF7C3AED))
    val Spiritual     = CategoryVisual("spiritual",     "Spiritual",     "⛪",  Color(0xFFFEE2E2), Color(0xFF9A3412))
    val Nature        = CategoryVisual("nature",        "Nature",        "🌳",  Color(0xFFDCFCE7), Color(0xFF16A34A))
    val Dining        = CategoryVisual("dining",        "Dining",        "🍽️", Color(0xFFFED7AA), Color(0xFFEA580C))
    val Sweets        = CategoryVisual("sweets",        "Sweets",        "🍩",  Color(0xFFFEF3C7), Color(0xFFD97706))
    val Coffee        = CategoryVisual("coffee",        "Coffee",        "☕",  Color(0xFFE7D3BB), Color(0xFF92400E))
    val Nightlife     = CategoryVisual("nightlife",     "Nightlife",     "🍹",  Color(0xFFE0E7FF), Color(0xFF4F46E5))
    val Entertainment = CategoryVisual("entertainment", "Fun",           "🎡",  Color(0xFFF3E8FF), Color(0xFF9333EA))
    val Shopping      = CategoryVisual("shopping",      "Shopping",      "🛍️", Color(0xFFFCE7F3), Color(0xFFDB2777))
    val Accommodation = CategoryVisual("accommodation", "Accommodation", "🏨",  Color(0xFFCCFBF1), Color(0xFF0D9488))
    val Transport     = CategoryVisual("transport",     "Transport",     "🚆",  Color(0xFFF1F5F9), Color(0xFF475569))
    val Other         = CategoryVisual("other",         "Other",         "📍",  Color(0xFFF1F5F9), Color(0xFF475569))

    val all = listOf(
        Sights, Museums, Spiritual, Nature, Dining, Sweets,
        Coffee, Nightlife, Entertainment, Shopping, Accommodation, Transport,
    )

    /** Map a Foursquare-human-category string onto one of our visuals. */
    fun forLabel(label: String): CategoryVisual {
        val t = label.lowercase()
        return when {
            "museum"  in t || "gallery"  in t || "art"       in t -> Museums
            "church"  in t || "mosque"   in t || "temple"    in t || "synagogue" in t || "shrine" in t -> Spiritual
            "park"    in t || "garden"   in t || "nature"    in t || "trail"     in t || "beach"  in t || "forest" in t -> Nature
            "coffee"  in t || "café"     in t || "cafe"      in t -> Coffee
            "dessert" in t || "bakery"   in t || "ice cream" in t || "sweet"     in t || "pastry" in t -> Sweets
            "restaurant" in t || "food"  in t || "eatery"    in t || "cuisine"   in t || "bistro" in t || "diner" in t -> Dining
            "bar"     in t || "pub"      in t || "night"     in t || "club"      in t || "lounge" in t -> Nightlife
            "amusement" in t || "zoo"    in t || "aquarium"  in t || "water park" in t || "theme park" in t -> Entertainment
            "shop"    in t || "store"    in t || "market"    in t || "mall"      in t || "boutique" in t -> Shopping
            "hotel"   in t || "hostel"   in t || "resort"    in t || "lodge"     in t || "motel"   in t -> Accommodation
            "airport" in t || "railway"  in t || "metro"     in t || "bus"       in t || "station" in t || "transit" in t -> Transport
            "monument" in t || "historic" in t || "landmark" in t || "sight"    in t || "plaza"   in t || "square"  in t -> Sights
            else -> Sights
        }
    }

    /** The Foursquare category-id string matching this visual, or null if there isn't one. */
    fun fsqId(visual: CategoryVisual): String? = when (visual.id) {
        "sights"        -> FoursquareCategories.SIGHTS
        "museums"       -> FoursquareCategories.MUSEUMS
        "spiritual"     -> FoursquareCategories.SPIRITUAL
        "nature"        -> FoursquareCategories.NATURE
        "dining"        -> FoursquareCategories.RESTAURANTS
        "sweets"        -> FoursquareCategories.SWEETS
        "coffee"        -> FoursquareCategories.COFFEE
        "nightlife"     -> FoursquareCategories.NIGHTLIFE
        "entertainment" -> FoursquareCategories.ENTERTAINMENT
        "shopping"      -> FoursquareCategories.SHOPPING
        "accommodation" -> FoursquareCategories.ACCOMMODATION
        "transport"     -> FoursquareCategories.TRANSPORT
        else            -> null
    }
}

// ── Main screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    tripId: String,
    modifier: Modifier = Modifier,
    initialDestination: String = "",
    onDestinationChanged: (String) -> Unit = {},
    viewModel: PlaceViewModel = koinViewModel(),
) {
    val s        = LocalStrings.current
    val isOnline = LocalIsOnline.current
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog     by remember { mutableStateOf(false) }
    var editingPlace       by remember { mutableStateOf<Place?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showSearchSheet    by remember { mutableStateOf(false) }

    // loadPlaces is driven entirely by TripDetailScreen's LaunchedEffects so that
    // the call always uses the correct, non-stale initialDestination. Calling it
    // here with a default-parameter would capture a stale value from the frame
    // before loadTrip's synchronous reset takes effect.

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            // 1) No destination yet — prompt user to pick one.
            uiState.locationQuery.isBlank() -> {
                ChooseDestinationHero(
                    onChooseClick = { showLocationDialog = true },
                )
            }
            // 2) Loading spinner while Firestore flow warms up.
            uiState.isLoading && uiState.places.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            // 3) Saved list (possibly empty state), with floating search FAB.
            else -> {
                SavedPlacesContent(
                    places              = uiState.places,
                    locationLabel       = uiState.locationQuery,
                    onChangeLocation    = { showLocationDialog = true },
                    onEditPlace         = { place -> editingPlace = place; showEditDialog = true },
                    onDeletePlace       = { place -> viewModel.deletePlace(tripId, place.id) },
                    onSearchClick       = { showSearchSheet = true },
                    isOnline            = isOnline,
                )
            }
        }

        // ── Floating search FAB ─────────────────────────────────────────
        if (uiState.locationQuery.isNotBlank()) {
            ExtendedFloatingActionButton(
                onClick         = { if (isOnline) showSearchSheet = true },
                modifier        = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacing.xl)
                    .alpha(if (isOnline) 1f else 0.38f),
                shape           = AppShape.pill,
                containerColor  = MaterialTheme.colorScheme.primary,
                contentColor    = MaterialTheme.colorScheme.onPrimary,
                icon            = { Icon(Icons.Default.Search, contentDescription = null) },
                text            = { Text("Discover", fontWeight = FontWeight.Bold) },
            )
        }

        // ── Error snackbar — only shown when the search sheet is NOT open.
        // When the sheet is open, errors are shown inline inside the sheet.
        if (!showSearchSheet) {
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(AppSpacing.lg),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) { Text(s.generalDismiss) }
                    },
                ) { Text(error) }
            }
        }
    }

    // ── Modals ──────────────────────────────────────────────────────────
    if (showLocationDialog) {
        ChangeLocationDialog(
            currentLocation = uiState.locationQuery,
            onDismiss       = { showLocationDialog = false },
            onConfirm       = { newLocation ->
                viewModel.updateLocationQuery(newLocation)
                onDestinationChanged(newLocation) // persist back to the trip
                showLocationDialog = false
            },
            isSearching = uiState.isSearching,
        )
    }

    if (showSearchSheet) {
        SearchPlacesSheet(
            locationLabel    = uiState.locationQuery,
            query            = uiState.searchQuery,
            selectedCategory = uiState.selectedCategoryId,
            isSearching      = uiState.isSearching,
            isSaving         = uiState.isSaving,
            results          = uiState.searchResults,
            savedPlaces      = uiState.places,
            canLoadMore      = uiState.canLoadMore,
            error            = uiState.error,
            onQueryChange    = { viewModel.updateSearchQuery(it) },
            onCategoryChange = { fsqId ->
                viewModel.setCategoryFilter(fsqId)
                viewModel.searchPlaces()
            },
            onSearch         = { viewModel.searchPlaces() },
            onLoadMore       = { viewModel.loadMoreSearchResults() },
            onAddPlace       = { place -> viewModel.addPlace(tripId, place) },
            onErrorDismiss   = { viewModel.clearError() },
            onDismiss        = {
                viewModel.clearSearchResults()
                showSearchSheet = false
            },
        )
    }

    if (showEditDialog && editingPlace != null) {
        EditPlaceDialog(
            place   = editingPlace!!,
            onDismiss = { showEditDialog = false; editingPlace = null },
            onConfirm = { updatedPlace ->
                viewModel.updatePlace(tripId, updatedPlace)
                showEditDialog = false
                editingPlace = null
            },
            isSaving = uiState.isSaving,
        )
    }
}

// ── Hero state: no destination yet ──────────────────────────────────────────

@Composable
private fun ChooseDestinationHero(onChooseClick: () -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(AppSpacing.xxxl),
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
                    imageVector        = Icons.Outlined.Explore,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier           = Modifier.size(40.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        Text(
            text       = s.placesWhereTo,
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text      = s.placesSetDestHint,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        GradientButton(
            text    = s.placesChooseDestination,
            onClick = onChooseClick,
        )
    }
}

// ── Loaded state: destination chip + saved-places list grouped by category ──

@Composable
private fun SavedPlacesContent(
    places:           List<Place>,
    locationLabel:    String,
    onChangeLocation: () -> Unit,
    onEditPlace:      (Place) -> Unit,
    onDeletePlace:    (Place) -> Unit,
    onSearchClick:    () -> Unit,
    isOnline:         Boolean = true,
) {
    // Group places by their mapped category visual (preserves insertion order).
    val grouped = remember(places) {
        places.groupBy { CategoryCatalog.forLabel(it.category) }
            .toList()
            .sortedBy { (visual, _) -> CategoryCatalog.all.indexOf(visual).let { if (it == -1) Int.MAX_VALUE else it } }
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(
            start  = AppSpacing.lg,
            end    = AppSpacing.lg,
            top    = AppSpacing.md,
            bottom = AppSpacing.xxxl + AppSpacing.xxl, // room for the FAB
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        // Destination / context strip
        item { DestinationPill(label = locationLabel, onClick = onChangeLocation) }

        if (places.isEmpty()) {
            item {
                EmptyPlacesState(
                    onDiscoverClick = onSearchClick,
                    modifier        = Modifier.fillParentMaxHeight(0.7f),
                )
            }
        } else {
            grouped.forEach { (visual, placesInCat) ->
                item(key = "header-${visual.id}") {
                    CategorySectionHeader(visual = visual, count = placesInCat.size)
                }
                items(items = placesInCat, key = { it.id }) { place ->
                    PlaceCard(
                        place    = place,
                        onEdit   = { onEditPlace(place) },
                        onDelete = { onDeletePlace(place) },
                        isOnline = isOnline,
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationPill(label: String, onClick: () -> Unit) {
    val s = LocalStrings.current
    Surface(
        shape    = AppShape.pill,
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Surface(
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = s.placesExploring,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector        = Icons.Default.Edit,
                contentDescription = s.placesChangeDestination,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CategorySectionHeader(visual: CategoryVisual, count: Int) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.sm, bottom = AppSpacing.xs),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Surface(
            shape    = AppShape.sm,
            color    = visual.tintBg,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                EmojiText(text = visual.emoji, fontSize = 16.sp)
            }
        }
        Text(
            text       = visual.label,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Surface(
            shape = AppShape.sm,
            color = visual.tintBg,
        ) {
            Text(
                text       = "$count",
                color      = visual.accent,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = AppSpacing.sm, vertical = 2.dp),
            )
        }
    }
}

// ── Saved-place card (new Figma-style layout) ───────────────────────────────

@Composable
fun PlaceCard(
    place:    Place,
    onEdit:   () -> Unit,
    onDelete: () -> Unit,
    isOnline: Boolean = true,
) {
    val s      = LocalStrings.current
    val visual = remember(place.category) { CategoryCatalog.forLabel(place.category) }

    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
    ) {
        Column(
            modifier            = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Row(
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                // Category icon chip
                Surface(
                    shape    = AppShape.md,
                    color    = visual.tintBg,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        EmojiText(text = visual.emoji, fontSize = 26.sp)
                    }
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    EmojiText(
                        text       = place.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        CategoryBadge(visual = visual, label = place.category.ifBlank { visual.label })

                        if (place.rating > 0) {
                            RatingPill(rating = place.rating)
                        }

                        if (place.distanceMeters > 0) {
                            Text(
                                text     = formatDistance(place.distanceMeters),
                                fontSize = 11.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (place.address.isNotBlank()) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            modifier              = Modifier.padding(top = 2.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier           = Modifier.size(12.dp),
                            )
                            EmojiText(
                                text     = place.address,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Inline actions
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = s.generalEdit,
                            tint               = Amber600,
                            modifier           = Modifier.size(16.dp),
                        )
                    }
                    IconButton(
                        onClick  = onDelete,
                        enabled  = isOnline,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.DeleteOutline,
                            contentDescription = s.generalRemove,
                            tint               = if (isOnline) MaterialTheme.colorScheme.error
                                                 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Inline note preview
            if (place.notes.isNotBlank()) {
                InlineNote(note = place.notes, onClick = onEdit)
            }
        }
    }
}

@Composable
private fun CategoryBadge(visual: CategoryVisual, label: String) {
    Surface(
        shape = AppShape.sm,
        color = visual.tintBg,
    ) {
        Text(
            text       = label,
            color      = visual.accent,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.padding(horizontal = AppSpacing.sm, vertical = 2.dp),
        )
    }
}

@Composable
private fun RatingPill(rating: Double) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector        = Icons.Default.Star,
            contentDescription = null,
            tint               = Amber500,
            modifier           = Modifier.size(12.dp),
        )
        Text(
            text       = formatRating(rating),
            color      = Color(0xFFB45309),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InlineNote(note: String, onClick: () -> Unit) {
    Surface(
        shape    = AppShape.sm,
        color    = Color(0xFFFFFBEB),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = AppSpacing.sm + 2.dp, vertical = AppSpacing.sm),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs + 2.dp),
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.StickyNote2,
                contentDescription = null,
                tint               = Amber500,
                modifier           = Modifier.size(14.dp).padding(top = 1.dp),
            )
            EmojiText(
                text     = note,
                color    = Color(0xFF92400E),
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyPlacesState(onDiscoverClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(
        modifier            = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape    = AppShape.xl,
            color    = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = Icons.Default.Explore,
                    contentDescription = null,
                    modifier           = Modifier.size(40.dp),
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        Text(text = s.placesNoPlaces, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text      = s.placesNoPlacesBody,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Search sheet ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPlacesSheet(
    locationLabel:    String,
    query:            String,
    selectedCategory: String?,
    isSearching:      Boolean,
    isSaving:         Boolean,
    results:          List<Place>,
    savedPlaces:      List<Place>,
    canLoadMore:      Boolean,
    error:            String?,
    onQueryChange:    (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onSearch:         () -> Unit,
    onLoadMore:       () -> Unit,
    onAddPlace:       (Place) -> Unit,
    onErrorDismiss:   () -> Unit,
    onDismiss:        () -> Unit,
) {
    val s          = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-search on first open so the user sees popular spots immediately.
    LaunchedEffect(Unit) {
        if (results.isEmpty()) onSearch()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = AppShape.bottomSheet,
    ) {
        PopupThemeProvider {
        // The sheet uses fillMaxHeight so the results list always has room to
        // render — without this the fixed-size items above the list can push
        // it below the visible fold on smaller screens.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)              // leave a sliver at top for the drag handle
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = s.placesDiscoverEyebrow,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Black,
                        color         = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text       = s.placesPopularIn(locationLabel),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = s.placesCloseSearch)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // ── Search field ──────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = onQueryChange,
                placeholder   = { Text(s.placesSearchPlaceholder) },
                leadingIcon   = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = if (query.isNotBlank()) {
                    { IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = s.generalClose)
                    } }
                } else null,
                singleLine    = true,
                shape         = AppShape.lg,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                ),
                modifier        = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch() },
                ),
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // ── Category chips ────────────────────────────────────────────
            // Fixed-size list (12 categories) — Row+horizontalScroll lets us add
            // draggable for mouse-drag-to-scroll support on the web.
            val catScrollState = rememberScrollState()
            val catScope       = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(catScrollState)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            catScope.launch { catScrollState.scrollBy(-delta) }
                        },
                    )
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                CategoryCatalog.all.forEach { visual ->
                    val fsqId    = CategoryCatalog.fsqId(visual)
                    val isActive = fsqId != null && fsqId == selectedCategory
                    CategoryFilterChip(
                        visual   = visual,
                        isActive = isActive,
                        onClick  = { onCategoryChange(if (isActive) null else fsqId) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // ── Search CTA ────────────────────────────────────────────────
            GradientButton(
                text      = if (isSearching) s.placesSearching else s.placesSearch,
                onClick   = onSearch,
                enabled   = !isSearching,
                isLoading = isSearching,
            )

            // ── Inline error banner (shown inside the sheet) ──────────────
            if (error != null) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShape.lg)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Text(
                        text     = error,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick  = onErrorDismiss,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = s.generalDismiss,
                            tint     = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = AppSpacing.sm),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )

            // ── Results (fills remaining height) ─────────────────────────
            // Already-saved places are hidden from the list with an
            // AnimatedVisibility-driven shrink/fade so the card the user just
            // tapped "Add" on slides out instead of switching to a checkmark.
            val savedFoursquareIds = remember(savedPlaces) {
                savedPlaces.mapNotNull { it.foursquareId.takeIf { id -> id.isNotEmpty() } }.toSet()
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    isSearching && results.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    results.isEmpty() ->
                        Column(
                            modifier            = Modifier.align(Alignment.Center).padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        ) {
                            Text("🧭", fontSize = 40.sp)
                            Text(
                                text       = s.placesNoMatches,
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text      = s.placesNoMatchesHint,
                                style     = MaterialTheme.typography.bodySmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                    else ->
                        LazyColumn(
                            modifier            = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            contentPadding      = PaddingValues(bottom = AppSpacing.md),
                        ) {
                            items(
                                items = results,
                                // Unique key: prefer stable foursquareId, fall back to name
                                // so duplicate names don't cause key collisions.
                                key = { place ->
                                    place.foursquareId.ifEmpty { "${place.name}_${place.latitude}_${place.longitude}" }
                                },
                            ) { place ->
                                val isHidden = place.foursquareId.isNotEmpty() &&
                                        place.foursquareId in savedFoursquareIds
                                this@Column.AnimatedVisibility(
                                    visible = !isHidden,
                                    enter   = fadeIn() + expandVertically(),
                                    exit    = fadeOut() + shrinkVertically(),
                                ) {
                                    SearchResultCard(
                                        place    = place,
                                        isAdding = isSaving,
                                        onAdd    = { onAddPlace(place) },
                                    )
                                }
                            }

                            // Footer: "Load more" CTA — shown only when the previous
                            // page came back full and we're still under Foursquare's
                            // hard 50-result cap.
                            if (canLoadMore) {
                                item(key = "load-more-footer") {
                                    Row(
                                        modifier              = Modifier
                                            .fillMaxWidth()
                                            .padding(top = AppSpacing.sm),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        OutlinedButton(
                                            onClick = onLoadMore,
                                            enabled = !isSearching,
                                            shape   = AppShape.pill,
                                        ) {
                                            if (isSearching) {
                                                CircularProgressIndicator(
                                                    modifier    = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                )
                                                Spacer(Modifier.width(AppSpacing.sm))
                                            }
                                            Text(s.placesLoadMore)
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
}

@Composable
private fun CategoryFilterChip(
    visual:   CategoryVisual,
    isActive: Boolean,
    onClick:  () -> Unit,
) {
    val bg     = if (isActive) visual.accent else visual.tintBg
    val fg     = if (isActive) Color.White else visual.accent
    val border = if (isActive) visual.accent else visual.accent.copy(alpha = 0.2f)

    Row(
        modifier              = Modifier
            .clip(AppShape.pill)
            .background(bg)
            .border(1.dp, border, AppShape.pill)
            .clickable { onClick() }
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs + 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        EmojiText(text = visual.emoji, fontSize = 13.sp)
        Text(
            text       = visual.label,
            color      = fg,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SearchResultCard(
    place:    Place,
    isAdding: Boolean,
    onAdd:    () -> Unit,
) {
    val s      = LocalStrings.current
    val visual = remember(place.category) { CategoryCatalog.forLabel(place.category) }

    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
    ) {
        Row(
            modifier              = Modifier.padding(AppSpacing.md),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            // Icon chip
            Surface(
                shape    = AppShape.md,
                color    = visual.tintBg,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    EmojiText(text = visual.emoji, fontSize = 26.sp)
                }
            }

            // Content
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                EmojiText(
                    text       = place.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    CategoryBadge(visual = visual, label = place.category.ifBlank { visual.label })
                    if (place.rating > 0) {
                        RatingPill(rating = place.rating)
                    }
                    if (place.distanceMeters > 0) {
                        Text(
                            text     = formatDistance(place.distanceMeters),
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (place.address.isNotBlank()) {
                    EmojiText(
                        text     = place.address,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Save action — already-saved places are filtered out of the
            // results list at the call site, so this card always offers "Add".
            Surface(
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primary,
                onClick  = { if (!isAdding) onAdd() },
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Default.Add,
                        contentDescription = s.placesSavePlace,
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ── Edit / location dialogs ─────────────────────────────────────────────────

@Composable
fun ChangeLocationDialog(
    currentLocation: String,
    onDismiss:       () -> Unit,
    onConfirm:       (String) -> Unit,
    isSearching:     Boolean,
) {
    var tempLocation by remember { mutableStateOf(currentLocation) }
    AlertDialog(
        onDismissRequest = { if (!isSearching) onDismiss() },
        title = { Text("Where are you exploring?") },
        shape = AppShape.xl,
        text = {
            OutlinedTextField(
                value         = tempLocation,
                onValueChange = { tempLocation = it },
                label         = { Text("City or address") },
                singleLine    = true,
                shape         = AppShape.lg,
                modifier      = Modifier.fillMaxWidth(),
                enabled       = !isSearching,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempLocation) },
                shape   = AppShape.lg,
                enabled = tempLocation.isNotBlank() && !isSearching,
            ) { Text("Set Location") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSearching) { Text("Cancel") }
        },
    )
}

@Composable
fun EditPlaceDialog(
    place:     Place,
    onDismiss: () -> Unit,
    onConfirm: (Place) -> Unit,
    isSaving:  Boolean,
) {
    var notes by remember { mutableStateOf(place.notes) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title            = { Text("Edit notes") },
        shape            = AppShape.xl,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                EmojiText(text = place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Notes, tips, reservations…") },
                    minLines      = 3,
                    maxLines      = 5,
                    shape         = AppShape.lg,
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !isSaving,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(place.copy(notes = notes)) },
                shape   = AppShape.lg,
                enabled = !isSaving,
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(AppSpacing.lg), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        },
    )
}

// ── Small helpers ───────────────────────────────────────────────────────────

private fun formatRating(rating: Double): String {
    // Foursquare returns 0.0 – 10.0; keep one decimal.
    val rounded = (rating * 10).toLong() / 10.0
    return rounded.toString()
}

private fun formatDistance(meters: Int): String = when {
    meters < 1000 -> "$meters m"
    else          -> "${(meters / 100) / 10.0} km"
}
