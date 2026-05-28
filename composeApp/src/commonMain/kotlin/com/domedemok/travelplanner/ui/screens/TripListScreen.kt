package com.domedemok.travelplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.components.LocalIsOnline
import com.domedemok.travelplanner.ui.components.NewTripFlow
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.TripViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun TripListScreen(
    onTripClick: (String) -> Unit,
    onJoinTrip: () -> Unit = {},
    tripViewModel: TripViewModel = koinViewModel(),
) {
    val s = LocalStrings.current
    val isOnline = LocalIsOnline.current
    val tripUiState by tripViewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var fabExpanded     by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // No manual loadTrips() needed here — TripViewModel.observeTrips() opens a
    // Firestore real-time listener in init{} and keeps the state up to date
    // automatically for the entire lifetime of the ViewModel.

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            tripUiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            tripUiState.trips.isEmpty() -> {
                EmptyTripsPlaceholder(
                    modifier = Modifier.align(Alignment.Center),
                    onCreateClick = { showCreateDialog = true },
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start  = AppSpacing.lg,
                        end    = AppSpacing.lg,
                        top    = AppSpacing.lg,
                        bottom = AppSpacing.xxxl + AppSpacing.xl, // leave room for FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    // ── Section header ────────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = AppSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text       = s.tripListTitle,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.onBackground,
                            )
                            Surface(
                                shape = AppShape.pill,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text     = "${tripUiState.trips.size}",
                                    modifier = Modifier.padding(
                                        horizontal = AppSpacing.md,
                                        vertical   = AppSpacing.xs,
                                    ),
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    itemsIndexed(tripUiState.trips, key = { _, trip -> trip.id }) { index, trip ->
                        val isFavorite = tripUiState.favoriteTripIds.contains(trip.id)
                        TripCard(
                            trip            = trip,
                            isFavorite      = isFavorite,
                            onClick         = { onTripClick(trip.id) },
                            onFavoriteClick = { tripViewModel.toggleFavorite(trip.id, isFavorite) },
                        )
                    }
                }
            }
        }

        // ── Scrim — dismiss speed dial on tap outside ─────────────────────────
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(
                        indication          = null,
                        interactionSource   = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) { fabExpanded = false },
            )
        }

        // ── Speed Dial FAB ────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            horizontalAlignment = Alignment.End,
        ) {
            // ── Sub-items (visible when expanded) ────────────────────────────
            AnimatedVisibility(
                visible = fabExpanded && isOnline,
                enter   = fadeIn() + slideInVertically { it / 2 },
                exit    = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    horizontalAlignment = Alignment.End,
                ) {
                    // Join a Trip
                    SpeedDialItem(
                        icon    = Icons.Default.QrCodeScanner,
                        label   = s.joinTripButton,
                        onClick = {
                            fabExpanded = false
                            scope.launch { delay(180); onJoinTrip() }
                        },
                    )
                    // Create a Trip
                    SpeedDialItem(
                        icon    = Icons.Default.AddCircleOutline,
                        label   = s.tripListCreateButton,
                        onClick = {
                            fabExpanded = false
                            scope.launch { delay(180); showCreateDialog = true }
                        },
                    )
                }
            }

            // ── Main FAB ─────────────────────────────────────────────────────
            FloatingActionButton(
                onClick        = { if (isOnline) fabExpanded = !fabExpanded },
                modifier       = Modifier.alpha(if (isOnline) 1f else 0.38f),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector        = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = s.tripListCreateButton,
                )
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        tripUiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AppSpacing.lg),
                action = {
                    TextButton(onClick = { tripViewModel.clearError() }) { Text(s.generalDismiss) }
                },
            ) { Text(error) }
        }
    }

    if (showCreateDialog && isOnline) {
        NewTripFlow(
            onDismiss = { showCreateDialog = false },
            onCreate  = { name, destination, description, startDate, endDate ->
                tripViewModel.createTrip(name, destination, description, startDate, endDate)
                showCreateDialog = false
            },
            isLoading = tripUiState.isCreatingTrip,
        )
    }
}

// ── Speed Dial item row ───────────────────────────────────────────────────────

@Composable
private fun SpeedDialItem(
    icon:    ImageVector,
    label:   String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        // Label chip
        Surface(
            shape  = AppShape.pill,
            color  = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Text(
                text       = label,
                modifier   = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
        }
        // Mini FAB
        SmallFloatingActionButton(
            onClick        = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyTripsPlaceholder(
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit,
) {
    val s = LocalStrings.current
    Column(
        modifier = modifier.padding(AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Surface(
            shape = AppShape.xl,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text       = s.tripListEmptyTitle,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text  = s.tripListEmptyBody,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        GradientButton(
            text     = s.tripListCreateButton,
            onClick  = onCreateClick,
            modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}

// ── Trip card ─────────────────────────────────────────────────────────────────

private enum class TripStatus { ACTIVE, UPCOMING, PAST }

@OptIn(ExperimentalTime::class)
@Composable
fun TripCard(
    trip: Trip,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val heroGradients = listOf(
        AppGradients.vibeBeach,
        AppGradients.vibeMountain,
        AppGradients.vibeCulture,
        AppGradients.vibeFoodie,
    )
    val heroGradient = heroGradients[abs(trip.id.hashCode()) % heroGradients.size]

    val currentTime = Clock.System.now().toEpochMilliseconds()
    val status = when {
        trip.endDate != 0L && trip.endDate < currentTime                          -> TripStatus.PAST
        trip.startDate != 0L && trip.startDate <= currentTime
                             && (trip.endDate == 0L || trip.endDate >= currentTime) -> TripStatus.ACTIVE
        else                                                                       -> TripStatus.UPCOMING
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape  = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        // ── Hero section ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(heroGradient),
            )
            // Dark overlay for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppGradients.tripCardOverlay),
            )

            val s = LocalStrings.current
            // Status badge — top start
            val (statusLabel, statusContainer, statusOnContainer) = when (status) {
                TripStatus.ACTIVE   -> Triple(
                    s.tripStatusActive,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
                TripStatus.UPCOMING -> Triple(
                    s.tripStatusUpcoming,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
                TripStatus.PAST     -> Triple(
                    s.tripStatusPast,
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppSpacing.md),
                shape = AppShape.pill,
                color = statusContainer,
            ) {
                Text(
                    text     = statusLabel,
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color    = statusOnContainer,
                )
            }

            // Favorite button — top end, with semi-transparent backdrop
            Box(
                modifier          = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppSpacing.sm),
                contentAlignment  = Alignment.Center,
            ) {
                Surface(
                    shape = AppShape.pill,
                    color = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier.size(34.dp),
                ) {}
                IconButton(
                    onClick  = onFavoriteClick,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector        = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = s.sideNavFavorite,
                        tint               = if (isFavorite) HeartRed else Color.White.copy(alpha = 0.90f),
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }

            // Trip name + member count — bottom start
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Text(
                    text       = trip.name,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    if (trip.destination.isNotEmpty()) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier           = Modifier.size(13.dp),
                                tint               = Color.White.copy(alpha = 0.8f),
                            )
                            Text(
                                text     = trip.destination,
                                fontSize = 12.sp,
                                color    = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (trip.tripMembers.size > 1) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.People,
                                contentDescription = null,
                                modifier           = Modifier.size(13.dp),
                                tint               = Color.White.copy(alpha = 0.8f),
                            )
                            Text(
                                text     = s.tripCardMembers(trip.tripMembers.size),
                                fontSize = 12.sp,
                                color    = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }

        // ── Details section ───────────────────────────────────────────────────
        val hasDetails = trip.description.isNotEmpty() || (trip.startDate != 0L && trip.endDate != 0L)
        if (hasDetails) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                if (trip.description.isNotEmpty()) {
                    Text(
                        text     = trip.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (trip.startDate != 0L && trip.endDate != 0L) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier           = Modifier.size(14.dp),
                            tint               = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text     = "${formatTripDate(trip.startDate)} – ${formatTripDate(trip.endDate)}",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatTripDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val instant  = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.year}-${dateTime.month.number.toString().padStart(2, '0')}-${dateTime.day.toString().padStart(2, '0')}"
}
