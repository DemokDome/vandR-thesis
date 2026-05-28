package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.components.EmojiText
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.AuthViewModel
import com.domedemok.travelplanner.viewmodel.TripViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ExploreScreen(
    onTripClick: (String) -> Unit = {},
    tripViewModel: TripViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val s = LocalStrings.current
    val tripState by tripViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    // Most recently opened trip — prefers lastOpenedAt, falls back to updatedAt for
    // older trips that were created before the lastOpenedAt field was introduced.
    val lastTrip = tripState.trips
        .filter { it.updatedAt > 0L || it.lastOpenedAt > 0L }
        .maxByOrNull { it.lastOpenedAt.takeIf { ts -> ts > 0L } ?: it.updatedAt }

    val nowMs = Clock.System.now().toEpochMilliseconds()

    // Upcoming trips (start in the future)
    val upcomingTrips = tripState.trips
        .filter { it.startDate > nowMs }
        .sortedBy { it.startDate }
        .take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
    ) {
        // ── Greeting header ───────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(AppGradients.profileHeader),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.xl).padding(bottom = AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    val hour = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .hour
                    val greeting = when {
                        hour < 12 -> s.greetingMorning
                        hour < 17 -> s.greetingAfternoon
                        else      -> s.greetingEvening
                    }
                    val name = authState.currentUser?.displayName?.split(" ")?.firstOrNull() ?: s.profileDefaultName
                    Text(
                        text     = "$greeting, $name",
                        fontSize = 14.sp,
                        color    = Color.White.copy(alpha = 0.8f),
                    )
                    Text(
                        text       = s.exploreSubtitle,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        letterSpacing = (-0.3).sp,
                    )
                }
            }
        }

        // ── Last opened trip ──────────────────────────────────────────────────
        if (lastTrip != null) {
            item {
                Column(
                    modifier = Modifier.padding(
                        start = AppSpacing.xl,
                        end   = AppSpacing.xl,
                        top   = AppSpacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    SectionTitle(title = s.exploreContinue)

                    ElevatedCard(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .clickable { onTripClick(lastTrip.id) },
                        shape     = AppShape.xl,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
                        colors    = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppGradients.continuePlanning),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppGradients.tripCardOverlay),
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(AppSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            ) {
                                Surface(
                                    shape = AppShape.pill,
                                    color = Color.White.copy(alpha = 0.2f),
                                ) {
                                    Text(
                                        text     = s.exploreLastOpened,
                                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color    = Color.White,
                                    )
                                }
                                EmojiText(
                                    text       = lastTrip.name,
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open",
                                tint     = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(AppSpacing.md)
                                    .size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Upcoming trips ────────────────────────────────────────────────────
        if (upcomingTrips.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(top = AppSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    SectionTitle(
                        title    = s.exploreUpcoming,
                        modifier = Modifier.padding(horizontal = AppSpacing.xl),
                    )
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = AppSpacing.xl),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        items(upcomingTrips, key = { it.id }) { trip ->
                            UpcomingTripChip(
                                name    = trip.name,
                                onClick = { onTripClick(trip.id) },
                            )
                        }
                    }
                }
            }
        }

        // ── Inspiration — Vibes grid ──────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(
                    start = AppSpacing.xl,
                    end   = AppSpacing.xl,
                    top   = AppSpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                SectionTitle(title = s.exploreFindVibe)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    VibeCard(
                        label    = s.vibeBeach,
                        icon     = Icons.Default.BeachAccess,
                        gradient = AppGradients.vibeBeach,
                        modifier = Modifier.weight(1f),
                    )
                    VibeCard(
                        label    = s.vibeMountain,
                        icon     = Icons.Default.Terrain,
                        gradient = AppGradients.vibeMountain,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    VibeCard(
                        label    = s.vibeCulture,
                        icon     = Icons.Default.Museum,
                        gradient = AppGradients.vibeCulture,
                        modifier = Modifier.weight(1f),
                    )
                    VibeCard(
                        label    = s.vibeFoodie,
                        icon     = Icons.Default.Restaurant,
                        gradient = AppGradients.vibeFoodie,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Quick picks section (static inspiration) ──────────────────────────
        item {
            Column(
                modifier = Modifier.padding(
                    start = AppSpacing.xl,
                    end   = AppSpacing.xl,
                    top   = AppSpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                SectionTitle(title = s.travelTipsTitle)
                QuickPickCard(
                    title    = s.tipPlanAheadTitle,
                    subtitle = s.tipPlanAheadBody,
                    icon     = Icons.Default.Lightbulb,
                    gradient = AppGradients.vibeBeach,
                )
                QuickPickCard(
                    title    = s.tipInviteTitle,
                    subtitle = s.tipInviteBody,
                    icon     = Icons.Default.GroupAdd,
                    gradient = AppGradients.vibeMountain,
                )
                QuickPickCard(
                    title    = s.tipTrackTitle,
                    subtitle = s.tipTrackBody,
                    icon     = Icons.Default.AccountBalanceWallet,
                    gradient = AppGradients.vibeCulture,
                )
            }
        }
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text       = title,
        fontSize   = 18.sp,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onBackground,
        modifier   = modifier,
    )
}

@Composable
private fun UpcomingTripChip(name: String, onClick: () -> Unit) {
    val chipBg       = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape    = AppShape.lg,
        color    = chipBg,
        border   = BorderStroke(1.dp, contentColor.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint     = contentColor,
            )
            EmojiText(
                text       = name,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = contentColor,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VibeCard(
    label: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(90.dp)
            .background(gradient, AppShape.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint     = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        }
    }
}

@Composable
private fun QuickPickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = AppShape.lg,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(gradient, AppShape.md),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
