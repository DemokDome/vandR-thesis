package com.domedemok.travelplanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import com.domedemok.travelplanner.ui.theme.rememberCurrentDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.domedemok.travelplanner.data.model.ItineraryDay
import com.domedemok.travelplanner.data.model.Place
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ── Day colour palette ────────────────────────────────────────────────────────

/** Compose UI colours used for day chips and summary dots. */
private val dayComposeColors = listOf(
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

/** Google Maps hue values (0–360) matching [dayComposeColors]. */
private val dayMapHues = listOf(
    210f, // sky-blue  ≈ HUE_AZURE
    120f, // green     ≈ HUE_GREEN
    30f,  // orange    ≈ HUE_ORANGE
    270f, // violet    ≈ HUE_VIOLET
    330f, // pink      ≈ HUE_ROSE
    180f, // cyan      ≈ HUE_CYAN
    60f,  // yellow    ≈ HUE_YELLOW
    300f, // fuchsia   ≈ HUE_MAGENTA
    240f, // blue      ≈ HUE_BLUE
    0f,   // red       ≈ HUE_RED
)

private fun dayColor(dayIndex: Int): Color = dayComposeColors[dayIndex % dayComposeColors.size]
private fun dayHue(dayIndex: Int): Float  = dayMapHues[dayIndex % dayMapHues.size]

// ── Member location local model ───────────────────────────────────────────────

private data class MemberMarker(
    val userId: String,
    val name: String,
    val latLng: LatLng,
)

// ── Google Maps night-mode style ──────────────────────────────────────────────
// Standard "Aubergine" dark style from Google Maps Platform style wizard.
private const val DARK_MAP_STYLE = """[{"elementType":"geometry","stylers":[{"color":"#1d2c4d"}]},{"elementType":"labels.text.fill","stylers":[{"color":"#8ec3b9"}]},{"elementType":"labels.text.stroke","stylers":[{"color":"#1a3646"}]},{"featureType":"administrative.country","elementType":"geometry.stroke","stylers":[{"color":"#4b6878"}]},{"featureType":"administrative.land_parcel","elementType":"labels.text.fill","stylers":[{"color":"#64779e"}]},{"featureType":"administrative.province","elementType":"geometry.stroke","stylers":[{"color":"#4b6878"}]},{"featureType":"landscape.man_made","elementType":"geometry.stroke","stylers":[{"color":"#334e87"}]},{"featureType":"landscape.natural","elementType":"geometry","stylers":[{"color":"#023e58"}]},{"featureType":"poi","elementType":"geometry","stylers":[{"color":"#283d6a"}]},{"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#6f9ba5"}]},{"featureType":"poi","elementType":"labels.text.stroke","stylers":[{"color":"#1d2c4d"}]},{"featureType":"poi.park","elementType":"geometry.fill","stylers":[{"color":"#023e58"}]},{"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#3C7680"}]},{"featureType":"road","elementType":"geometry","stylers":[{"color":"#304a7d"}]},{"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#98a5be"}]},{"featureType":"road","elementType":"labels.text.stroke","stylers":[{"color":"#1d2c4d"}]},{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#2c6675"}]},{"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#255763"}]},{"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#b0d5ce"}]},{"featureType":"road.highway","elementType":"labels.text.stroke","stylers":[{"color":"#023747"}]},{"featureType":"transit","elementType":"labels.text.fill","stylers":[{"color":"#98a5be"}]},{"featureType":"transit","elementType":"labels.text.stroke","stylers":[{"color":"#1d2c4d"}]},{"featureType":"transit.line","elementType":"geometry.fill","stylers":[{"color":"#283d6a"}]},{"featureType":"transit.station","elementType":"geometry","stylers":[{"color":"#3a4762"}]},{"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e1626"}]},{"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#4e6d70"}]}]"""

// ── Actual composable ─────────────────────────────────────────────────────────

@Composable
actual fun MapScreen(
    places: List<Place>,
    itineraryDays: List<ItineraryDay>,
    tripId: String,
    currentUserId: String?,
    tripMembers: Map<String, String>,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasLocationPermission = it }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (places.isEmpty() && itineraryDays.isEmpty()) {
            EmptyMapState(modifier = Modifier.align(Alignment.Center))
        } else {
            GoogleMapView(
                places               = places,
                itineraryDays        = itineraryDays,
                tripId               = tripId,
                currentUserId        = currentUserId,
                tripMembers          = tripMembers,
                hasLocationPermission = hasLocationPermission,
                modifier             = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Main map view ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun GoogleMapView(
    places: List<Place>,
    itineraryDays: List<ItineraryDay>,
    tripId: String,
    currentUserId: String?,
    tripMembers: Map<String, String>,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val s              = LocalStrings.current
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore: FirebaseFirestore = koinInject()
    val isDarkTheme    = rememberCurrentDarkTheme()
    val mapStyle       = remember(isDarkTheme) {
        if (isDarkTheme) MapStyleOptions(DARK_MAP_STYLE) else null
    }

    // ── Selected day (null = show all) ────────────────────────────────────────
    var selectedDayId by remember { mutableStateOf<String?>(null) }
    var showSummary   by remember { mutableStateOf(false) }

    // ── Member locations ──────────────────────────────────────────────────────
    var memberMarkers by remember { mutableStateOf<List<MemberMarker>>(emptyList()) }

    // Listen to all member locations in this trip
    DisposableEffect(tripId) {
        if (tripId.isBlank()) return@DisposableEffect onDispose { }
        val reg = firestore
            .collection("trips").document(tripId)
            .collection("memberLocations")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                memberMarkers = snap.documents.mapNotNull { doc ->
                    val lat  = doc.getDouble("latitude")  ?: return@mapNotNull null
                    val lng  = doc.getDouble("longitude") ?: return@mapNotNull null
                    val uid  = doc.id
                    val name = doc.getString("name") ?: tripMembers[uid] ?: "?"
                    MemberMarker(uid, name, LatLng(lat, lng))
                }
            }
        onDispose { reg.remove() }
    }

    // Publish own location every 20 s while map is open
    LaunchedEffect(tripId, currentUserId, hasLocationPermission) {
        if (tripId.isBlank() || currentUserId == null || !hasLocationPermission) return@LaunchedEffect
        val fused = LocationServices.getFusedLocationProviderClient(context)
        while (true) {
            try {
                val loc = fused.lastLocation.await()
                if (loc != null) {
                    firestore.collection("trips").document(tripId)
                        .collection("memberLocations").document(currentUserId)
                        .set(mapOf(
                            "latitude"  to loc.latitude,
                            "longitude" to loc.longitude,
                            "name"      to (tripMembers[currentUserId] ?: ""),
                            "updatedAt" to System.currentTimeMillis(),
                        ))
                }
            } catch (_: Exception) { }
            delay(20_000)
        }
    }

    // ── Build day→place lookup ────────────────────────────────────────────────
    val placeById    = remember(places) { places.associateBy { it.id } }
    val sortedDays   = remember(itineraryDays) { itineraryDays.sortedBy { it.dayNumber } }

    // placeId → 0-based day index (for colouring in "All" view)
    val placeToDay = remember(sortedDays) {
        buildMap {
            sortedDays.forEachIndexed { idx, day ->
                day.placeIds.forEach { pid -> put(pid, idx) }
            }
        }
    }

    // Visible places based on selected day
    // "__unassigned__" is a sentinel that means "show only places not in any day"
    val visiblePlaces = remember(selectedDayId, sortedDays, places, placeById, placeToDay) {
        when (selectedDayId) {
            null             -> places
            "__unassigned__" -> places.filter { it.id !in placeToDay }
            else             -> sortedDays.find { it.id == selectedDayId }
                                    ?.placeIds?.mapNotNull { placeById[it] } ?: emptyList()
        }
    }
    val unassignedPlaces = remember(places, placeToDay) { places.filter { it.id !in placeToDay } }

    // ── Camera ────────────────────────────────────────────────────────────────
    val cameraPositionState = rememberCameraPositionState {
        val center = visiblePlaces.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
            ?: LatLng(47.4979, 19.0402)
        position = CameraPosition.fromLatLngZoom(center, 12f)
    }

    // Re-center when selected day changes
    LaunchedEffect(selectedDayId, visiblePlaces) {
        val center = visiblePlaces.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
            ?: return@LaunchedEffect
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, 13f))
    }

    // ── Summary sheet ─────────────────────────────────────────────────────────
    if (showSummary) {
        ModalBottomSheet(
            onDismissRequest = { showSummary = false },
            shape            = AppShape.bottomSheet,
            containerColor   = MaterialTheme.colorScheme.surface,
        ) {
            val unassigned = places.filter { it.id !in placeToDay }

            // LazyColumn so the list scrolls when there are many days.
            LazyColumn(
                modifier            = Modifier.fillMaxWidth(),
                contentPadding      = PaddingValues(
                    start  = AppSpacing.lg,
                    end    = AppSpacing.lg,
                    bottom = AppSpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                // ── Header ─────────────────────────────────────────────────
                item {
                    Text(
                        text       = s.mapDailySummary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp,
                        modifier   = Modifier.padding(top = AppSpacing.xs, bottom = AppSpacing.xs),
                    )
                }

                if (sortedDays.isEmpty()) {
                    item {
                        Text(
                            text  = s.mapNoDays,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    // ── Day cards ───────────────────────────────────────────
                    itemsIndexed(sortedDays) { idx, day ->
                        val dayPlaces = day.placeIds.mapNotNull { placeById[it] }
                        ElevatedCard(
                            shape     = AppShape.lg,
                            modifier  = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                            onClick   = {
                                selectedDayId = if (selectedDayId == day.id) null else day.id
                                showSummary   = false
                            },
                        ) {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(dayColor(idx), CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = s.mapDayLabel(idx + 1) +
                                                if (day.date > 0L) "  ·  ${formatDayDate(day.date, s)}" else "",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 14.sp,
                                    )
                                    if (dayPlaces.isNotEmpty()) {
                                        Text(
                                            text  = dayPlaces.joinToString(" · ") { it.name },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    } else {
                                        Text(
                                            text  = s.mapNoPlacesAssigned,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text       = "${dayPlaces.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 13.sp,
                                    color      = dayColor(idx),
                                )
                            }
                        }
                    }

                    // ── Unassigned ──────────────────────────────────────────
                    if (unassigned.isNotEmpty()) {
                        item {
                            ElevatedCard(
                                shape     = AppShape.lg,
                                modifier  = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                                onClick   = {
                                    selectedDayId = if (selectedDayId == "__unassigned__") null else "__unassigned__"
                                    showSummary   = false
                                },
                            ) {
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = s.mapUnassigned,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize   = 14.sp,
                                        )
                                        Text(
                                            text  = unassigned.joinToString(" · ") { it.name },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                    Text(
                                        text       = "${unassigned.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 13.sp,
                                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Close button ────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(AppSpacing.xs))
                    OutlinedButton(
                        onClick  = { showSummary = false },
                        shape    = AppShape.lg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    ) { Text(s.mapClose, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    // ── Map + overlays ────────────────────────────────────────────────────────
    Box(modifier = modifier) {
        GoogleMap(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(bottom = 80.dp), // leave room for chips
            cameraPositionState = cameraPositionState,
            properties          = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType             = MapType.NORMAL,
                mapStyleOptions     = mapStyle,
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled     = false,
                myLocationButtonEnabled = false,
                compassEnabled          = true,
                mapToolbarEnabled       = true,
            ),
        ) {
            // ── Place markers ─────────────────────────────────────────────
            visiblePlaces.forEach { place ->
                val dayIdx = placeToDay[place.id]
                val hue    = if (dayIdx != null) dayHue(dayIdx)
                             else BitmapDescriptorFactory.HUE_ORANGE
                Marker(
                    state   = MarkerState(position = LatLng(place.latitude, place.longitude)),
                    title   = place.name,
                    snippet = place.address,
                    icon    = BitmapDescriptorFactory.defaultMarker(hue),
                )
            }

            // ── Member location markers ───────────────────────────────────
            memberMarkers.forEach { member ->
                MarkerComposable(
                    keys    = arrayOf(member.userId, member.name),
                    state   = MarkerState(position = member.latLng),
                    title   = member.name,
                    anchor  = androidx.compose.ui.geometry.Offset(0.5f, 1f),
                ) {
                    MemberLocationMarker(name = member.name)
                }
            }
        }

        // ── My-location FAB ───────────────────────────────────────────────
        if (hasLocationPermission) {
            Surface(
                modifier        = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = AppSpacing.lg, end = AppSpacing.lg)
                    .size(44.dp),
                shape           = AppShape.pill,
                color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation  = 8.dp,
                shadowElevation = 8.dp,
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val fused = LocationServices.getFusedLocationProviderClient(context)
                            val loc   = try { fused.lastLocation.await() } catch (_: Exception) { null }
                            if (loc != null) {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        imageVector        = Icons.Default.MyLocation,
                        contentDescription = s.mapMyLocation,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Bottom overlay: summary button + day chips ────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            // Summary toggle — only shown if there are itinerary days
            if (sortedDays.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = AppSpacing.md),
                ) {
                    Surface(
                        shape           = AppShape.pill,
                        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation  = 4.dp,
                        shadowElevation = 4.dp,
                        onClick         = { showSummary = true },
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs + 2.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                modifier           = Modifier.size(16.dp),
                                tint               = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text       = s.mapSummaryButton,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Day filter chips
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // "All" chip
                FilterChip(
                    selected = selectedDayId == null,
                    onClick  = { selectedDayId = null },
                    label    = { Text(s.mapAllDays, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor     = Color.White,
                    ),
                    shape = AppShape.pill,
                )

                sortedDays.forEachIndexed { idx, day ->
                    val isSelected = selectedDayId == day.id
                    val color      = dayColor(idx)
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedDayId = if (isSelected) null else day.id },
                        label    = {
                            Text(
                                text       = s.mapDayLabel(idx + 1) + if (day.date > 0L) "\n${formatDayDateShort(day.date)}" else "",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp,
                            )
                        },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor     = Color.White,
                            labelColor             = color,
                        ),
                        border   = FilterChipDefaults.filterChipBorder(
                            enabled              = true,
                            selected             = isSelected,
                            borderColor          = color.copy(alpha = 0.5f),
                            selectedBorderColor  = color,
                        ),
                        shape = AppShape.pill,
                    )
                }

                // "Unassigned" chip — only shown when there are unassigned places
                if (unassignedPlaces.isNotEmpty()) {
                    val isSelected = selectedDayId == "__unassigned__"
                    val color      = MaterialTheme.colorScheme.outline
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedDayId = if (isSelected) null else "__unassigned__" },
                        label    = {
                            Text(
                                text       = s.mapUnassigned,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
                            labelColor             = color,
                        ),
                        border   = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = isSelected,
                            borderColor         = color.copy(alpha = 0.4f),
                            selectedBorderColor = color,
                        ),
                        shape = AppShape.pill,
                    )
                }
            }
        }
    }
}

// ── Member location marker composable ─────────────────────────────────────────

@Composable
private fun MemberLocationMarker(name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Surface(
            shape           = CircleShape,
            color           = Color(0xFF7C3AED),
            modifier        = Modifier.size(36.dp),
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text       = name.take(2).uppercase(),
                    color      = Color.White,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                )
            }
        }
        // Triangle pointer
        Box(
            modifier = Modifier
                .size(width = 10.dp, height = 5.dp)
                .background(color = Color.Transparent)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(path, color = Color(0xFF7C3AED))
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyMapState(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(
        modifier            = modifier.padding(AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Surface(
            shape    = AppShape.xl,
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector        = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier           = Modifier.size(40.dp),
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(text = s.mapNoPlacesTitle, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text      = s.mapNoPlacesBody,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Date helpers ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
private fun formatDayDate(timestamp: Long, strings: com.domedemok.travelplanner.i18n.Strings): String {
    if (timestamp == 0L) return ""
    val validTs  = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val instant  = Instant.fromEpochMilliseconds(validTs)
    val dt       = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${strings.monthShortNames[dt.month.ordinal]} ${dt.day}."
}

@OptIn(ExperimentalTime::class)
private fun formatDayDateShort(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val validTs  = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val instant  = Instant.fromEpochMilliseconds(validTs)
    val dt       = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.month.ordinal + 1}/${dt.day}"
}
