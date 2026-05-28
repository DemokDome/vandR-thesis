package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Trip
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.i18n.Strings
import com.domedemok.travelplanner.viewmodel.ItineraryUiState
import com.domedemok.travelplanner.viewmodel.PlaceUiState
import com.domedemok.travelplanner.viewmodel.TripDetailUiState
import com.domedemok.travelplanner.ui.components.LayoutMode
import com.domedemok.travelplanner.ui.components.LocalIsOnline
import com.domedemok.travelplanner.ui.components.LocalLayoutMode
import com.domedemok.travelplanner.ui.components.OfflineBanner
import com.domedemok.travelplanner.ui.components.TripBottomNav
import com.domedemok.travelplanner.ui.components.TripTab
import com.domedemok.travelplanner.ui.components.EditTripDialog
import com.domedemok.travelplanner.ui.components.ShareTripDialog
import com.domedemok.travelplanner.ui.components.ManageMembersSheet
import com.domedemok.travelplanner.ui.components.WebTripSideNav
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.ItineraryViewModel
import com.domedemok.travelplanner.viewmodel.PlaceViewModel
import com.domedemok.travelplanner.viewmodel.TripDetailViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: TripDetailViewModel = koinViewModel()
) {
    val s = LocalStrings.current
    val isOnline = LocalIsOnline.current
    val isExpandedLayout = LocalLayoutMode.current == LayoutMode.EXPANDED
    val uiState by viewModel.uiState.collectAsState()
    val placeViewModel: PlaceViewModel = koinViewModel()
    val placeUiState by placeViewModel.uiState.collectAsState()
    val itineraryViewModel: ItineraryViewModel = koinViewModel()
    val itineraryUiState by itineraryViewModel.uiState.collectAsState()
    val isOwner = uiState.trip?.createdBy == viewModel.currentUserId

    var showEditDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManageMembers by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(TripTab.PLACES) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    // Preserves each tab's scroll position, form state, etc. across switches.
    val tabStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
        // initialLocation will be overridden once the trip is loaded;
        // PlacesTab re-uses the same PlaceViewModel instance so the location
        // stays in sync without an extra LaunchedEffect.
        placeViewModel.loadPlaces(tripId)
    }

    // Once the trip data arrives, seed the Places location query if it is still blank.
    // Guard: skip if the loaded trip belongs to a different tripId (stale ViewModel state
    // from the previously opened trip before this trip's data has finished loading).
    val trip = uiState.trip
    LaunchedEffect(trip?.id, trip?.startDate, trip?.endDate, trip?.destination) {
        if (trip?.id != tripId) return@LaunchedEffect
        val dest = trip.destination
        if (dest.isNotBlank()) {
            placeViewModel.loadPlaces(tripId, dest)
        }
        itineraryViewModel.load(
            tripId      = tripId,
            startDate   = trip.startDate,
            endDate     = trip.endDate,
            savedPlaces = placeUiState.places,
        )
    }


    if (isExpandedLayout) {
        // ── Expanded layout: sidebar + content area ──────────────────────────
        Row(modifier = Modifier.fillMaxSize()) {
            WebTripSideNav(
                selectedTab        = selectedTab,
                onTabSelected      = { selectedTab = it },
                onBack             = onNavigateBack,
                trip               = uiState.trip,
                isOwner            = isOwner,
                isOnline           = isOnline,
                onManageMembers    = { showManageMembers = true },
                onEdit             = { showEditDialog = true },
                onShare            = { showShareDialog = true; viewModel.ensureJoinCode() },
                onDeleteOrLeave    = { if (isOwner) showDeleteDialog = true else showLeaveDialog = true },
                modifier           = Modifier.fillMaxHeight(),
            )

            VerticalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineBanner(isOnline = isOnline)
                    Box(modifier = Modifier.weight(1f)) {
                        TripTabContent(
                            selectedTab        = selectedTab,
                            tripId             = tripId,
                            uiState            = uiState,
                            placeUiState       = placeUiState,
                            itineraryUiState   = itineraryUiState,
                            itineraryViewModel = itineraryViewModel,
                            viewModel          = viewModel,
                            isOwner            = isOwner,
                            stateHolder        = tabStateHolder,
                        )
                    }
                }

                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        action = { TextButton(onClick = { viewModel.clearError() }) { Text(s.generalDismiss) } }
                    ) { Text(error) }
                }
            }
        }
    } else {
        // ── Compact layout: gradient header + bottom tab bar ─────────────────
        Scaffold(
            bottomBar = {
                if (uiState.trip != null) {
                    TripBottomNav(
                        selectedTab   = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                }
            },
            topBar = {
                Column {
                    OfflineBanner(isOnline = isOnline)
                    TripDetailHeader(
                        trip            = uiState.trip,
                        isOwner         = isOwner,
                        isOnline        = isOnline,
                        onBack          = onNavigateBack,
                        onMembersClick  = { showManageMembers = true },
                        onEdit          = { showEditDialog = true },
                        onShare         = { showShareDialog = true; viewModel.ensureJoinCode() },
                        onDeleteOrLeave = { if (isOwner) showDeleteDialog = true else showLeaveDialog = true },
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TripTabContent(
                    selectedTab        = selectedTab,
                    tripId             = tripId,
                    uiState            = uiState,
                    placeUiState       = placeUiState,
                    itineraryUiState   = itineraryUiState,
                    itineraryViewModel = itineraryViewModel,
                    viewModel          = viewModel,
                    isOwner            = isOwner,
                    stateHolder        = tabStateHolder,
                )

                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        action = { TextButton(onClick = { viewModel.clearError() }) { Text(s.generalDismiss) } }
                    ) { Text(error) }
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showManageMembers && uiState.trip != null) {
        ManageMembersSheet(
            trip           = uiState.trip!!,
            currentUserId  = viewModel.currentUserId ?: "",
            onDismiss      = { showManageMembers = false },
            onRemoveMember = { userIdToRemove ->
                viewModel.removeMember(userIdToRemove)
                showManageMembers = false
            },
            onRenameMember = { userId, newName ->
                viewModel.renameMember(userId, newName)
            },
        )
    }

    if (showEditDialog && uiState.trip != null) {
        EditTripDialog(
            trip = uiState.trip!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, description, startDate, endDate ->
                viewModel.updateTrip(name, description, startDate, endDate)
                showEditDialog = false
            },
            isLoading = uiState.isEditing
        )
    }

    if (showShareDialog) {
        ShareTripDialog(
            onDismiss = { showShareDialog = false },
            onConfirm = { email ->
                viewModel.shareTrip(email)
                showShareDialog = false
            },
            isLoading = uiState.isSharing,
            trip      = uiState.trip,
            joinCode  = uiState.resolvedJoinCode,
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            shape = AppShape.xl,
            title = { Text(s.tripDetailLeaveTitle) },
            text = { Text(s.tripDetailLeaveMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveTrip {
                            showLeaveDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s.tripDetailLeaveButton) }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text(s.generalCancel) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = AppShape.xl,
            title = { Text(s.tripDetailDeleteTitle(uiState.trip?.name ?: "")) },
            text = { Text(s.tripDetailDeleteMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrip {
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(s.generalDelete)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !uiState.isDeleting) { Text(s.generalCancel) }
            }
        )
    }
}


// ── Shared tab content (used by both Android Scaffold and Web Row layouts) ────

@Composable
private fun TripTabContent(
    selectedTab:        TripTab,
    tripId:             String,
    uiState:            TripDetailUiState,
    placeUiState:       PlaceUiState,
    itineraryUiState:   ItineraryUiState,
    itineraryViewModel: ItineraryViewModel,
    viewModel:          TripDetailViewModel,
    isOwner:            Boolean,
    stateHolder:        androidx.compose.runtime.saveable.SaveableStateHolder,
) {
    val s = LocalStrings.current
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.trip != null -> {
            // SaveableStateProvider preserves each tab's scroll/form state across switches,
            // mirroring the MainShell pattern for the main tabs.
            stateHolder.SaveableStateProvider(key = selectedTab) {
                when (selectedTab) {
                    TripTab.PLACES -> PlacesTab(
                        tripId               = tripId,
                        initialDestination   = uiState.trip.destination,
                        onDestinationChanged = { viewModel.updateDestination(it) },
                        modifier             = Modifier.fillMaxSize(),
                    )
                    TripTab.MAP -> MapTab(
                        places        = placeUiState.places,
                        itineraryDays = itineraryUiState.days,
                        tripId        = tripId,
                        currentUserId = viewModel.currentUserId,
                        tripMembers   = uiState.trip.tripMembers,
                        modifier      = Modifier.fillMaxSize(),
                    )
                    TripTab.CHAT -> ChatTab(tripId = tripId, modifier = Modifier.fillMaxSize())
                    TripTab.BUDGET -> BudgetScreen(
                        tripId        = tripId,
                        tripMembers   = uiState.trip.tripMembers,
                        formerMembers = uiState.trip.formerMembers,
                        modifier      = Modifier.fillMaxSize(),
                    )
                    TripTab.ITINERARY -> ItineraryScreen(
                        tripId    = tripId,
                        trip      = uiState.trip,
                        viewModel = itineraryViewModel,
                        modifier  = Modifier.fillMaxSize(),
                    )
                    TripTab.GALLERY -> GalleryScreen(
                        tripId        = tripId,
                        currentUserId = viewModel.currentUserId,
                        isOwner       = isOwner,
                        modifier      = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = s.tripDetailNotFound)
            }
        }
    }
}

@Composable
fun PlacesTab(
    tripId: String,
    initialDestination: String = "",
    onDestinationChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PlacesScreen(
        tripId               = tripId,
        modifier             = modifier,
        initialDestination   = initialDestination,
        onDestinationChanged = onDestinationChanged,
    )
}

@Composable
fun ChatTab(tripId: String, modifier: Modifier = Modifier) { TripChatWrapperScreen(tripId = tripId) }

@Composable
fun MapTab(
    places: List<com.domedemok.travelplanner.data.model.Place>,
    itineraryDays: List<com.domedemok.travelplanner.data.model.ItineraryDay> = emptyList(),
    tripId: String = "",
    currentUserId: String? = null,
    tripMembers: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    MapScreen(
        places        = places,
        itineraryDays = itineraryDays,
        tripId        = tripId,
        currentUserId = currentUserId,
        tripMembers   = tripMembers,
        modifier      = modifier,
    )
}

@OptIn(ExperimentalTime::class)
private fun formatShortDate(timestamp: Long, strings: Strings): String {
    if (timestamp == 0L) return ""
    val instant  = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${strings.monthShortNames[dateTime.month.number - 1]} ${dateTime.day}"
}

private fun formatDateRange(trip: Trip, strings: Strings): String {
    if (trip.startDate == 0L && trip.endDate == 0L) return ""
    if (trip.startDate != 0L && trip.endDate != 0L) {
        return "${formatShortDate(trip.startDate, strings)} – ${formatShortDate(trip.endDate, strings)}"
    }
    return formatShortDate(if (trip.startDate != 0L) trip.startDate else trip.endDate, strings)
}

// ── Hero header ─────────────────────────────────────────────────────────────
// Gradient bar with back / title+dates / members + edit/share/delete-or-leave.
// Favourite lives on the trip card in TripListScreen — never in the detail header.
@Composable
private fun TripDetailHeader(
    trip:            Trip?,
    isOwner:         Boolean,
    isOnline:        Boolean,
    onBack:          () -> Unit,
    onMembersClick:  () -> Unit,
    onEdit:          () -> Unit,
    onShare:         () -> Unit,
    onDeleteOrLeave: () -> Unit,
) {
    val s = LocalStrings.current
    val memberCount = trip?.tripMembers?.size ?: 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppGradients.profileHeader)
            .padding(horizontal = AppSpacing.lg)
            .padding(top = AppSpacing.xxxl + AppSpacing.sm, bottom = AppSpacing.lg),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Back + title block ──────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier              = Modifier.weight(1f),
            ) {
                HeaderCircleButton(
                    icon        = Icons.AutoMirrored.Filled.ArrowBack,
                    description = s.generalBack,
                    onClick     = onBack,
                )
                Column {
                    Text(
                        text       = trip?.let { formatDateRange(it, s) } ?: "",
                        color      = Color.White.copy(alpha = 0.75f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text          = trip?.name ?: s.tripDetailFallback,
                        color         = Color.White,
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        maxLines      = 1,
                    )
                }
            }

            // ── Right-side actions ──────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs + 2.dp),
            ) {
                // Members pill
                if (trip != null) {
                    Surface(
                        shape   = AppShape.pill,
                        color   = Color.White.copy(alpha = 0.18f),
                        onClick = onMembersClick,
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = AppSpacing.sm + 2.dp, vertical = AppSpacing.xs + 2.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.People,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(14.dp),
                            )
                            Text(
                                text       = memberCount.toString(),
                                color      = Color.White,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Inline trip actions — replaces the old "MoreVert → bottom sheet"
                // pattern with three discoverable circle buttons. Edit / Share are
                // disabled while offline; Delete / Leave is destructive (red icon).
                if (trip != null) {
                    HeaderCircleButton(
                        icon        = Icons.Default.Edit,
                        description = s.tripDetailEditLabel,
                        onClick     = onEdit,
                        enabled     = isOnline,
                    )
                    HeaderCircleButton(
                        icon        = Icons.Default.PersonAdd,
                        description = s.tripDetailInviteLabel,
                        onClick     = onShare,
                        enabled     = isOnline,
                    )
                    HeaderCircleButton(
                        icon        = if (isOwner) Icons.Default.DeleteOutline
                                      else Icons.AutoMirrored.Filled.ExitToApp,
                        description = if (isOwner) s.tripDetailDeleteLabel else s.tripDetailLeaveLabel,
                        onClick     = onDeleteOrLeave,
                        enabled     = isOnline,
                        iconTint    = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    icon:        androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick:     () -> Unit,
    iconTint:    Color = Color.White,
    enabled:     Boolean = true,
) {
    Surface(
        shape    = CircleShape,
        color    = Color.White.copy(alpha = if (enabled) 0.18f else 0.08f),
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.size(36.dp).alpha(if (enabled) 1f else 0.45f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector        = icon,
                contentDescription = description,
                tint               = iconTint,
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}