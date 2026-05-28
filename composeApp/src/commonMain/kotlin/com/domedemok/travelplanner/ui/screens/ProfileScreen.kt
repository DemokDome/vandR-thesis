package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.domedemok.travelplanner.i18n.AppLanguage
import com.domedemok.travelplanner.i18n.LanguageManager
import com.domedemok.travelplanner.i18n.LocalStrings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.AuthViewModel
import com.domedemok.travelplanner.viewmodel.TripViewModel
import com.domedemok.travelplanner.viewmodel.resolve
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = koinViewModel(),
    tripViewModel: TripViewModel = koinViewModel(),
) {
    val s = LocalStrings.current
    val authState by authViewModel.uiState.collectAsState()
    val tripState by tripViewModel.uiState.collectAsState()
    val user = authState.currentUser
    val currentLanguage by LanguageManager.language.collectAsState()
    val currentTheme    by ThemeManager.themeMode.collectAsState()

    var showLogoutDialog     by remember { mutableStateOf(false) }
    var showDeleteDialog     by remember { mutableStateOf(false) }
    var showReAuthDialog     by remember { mutableStateOf(false) }
    var reAuthPassword       by remember { mutableStateOf("") }
    var showReAuthPassword   by remember { mutableStateOf(false) }

    // Surface-level re-auth trigger from ViewModel
    LaunchedEffect(authState.requiresReAuthForDeletion) {
        if (authState.requiresReAuthForDeletion) showReAuthDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Gradient header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(AppGradients.profileHeader),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                // Avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = user?.displayName
                        ?.split(" ")
                        ?.filter { it.isNotBlank() }
                        ?.take(2)
                        ?.joinToString("") { it.first().uppercase() }
                        ?: "?"
                    if (initials == "?") {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White,
                        )
                    } else {
                        Text(
                            text       = initials,
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                        )
                    }
                }

                Text(
                    text       = user?.displayName ?: s.profileDefaultName,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                Text(
                    text     = user?.email ?: "",
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.75f),
                )
            }
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            StatCard(
                label    = s.profileTripsLabel,
                value    = "${tripState.trips.size}",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = s.profileFavoritesLabel,
                value    = "${tripState.favoriteTripIds.size}",
                modifier = Modifier.weight(1f),
            )
            user?.createdAt?.let { createdAt ->
                StatCard(
                    label    = s.profileMemberSince,
                    value    = formatProfileDate(createdAt),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Account info section ──────────────────────────────────────────────
        ProfileSectionHeader(title = s.profileAccountSection)
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.xl),
            shape     = AppShape.xl,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
            colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)) {
                ProfileInfoRow(label = s.profileNameLabel,   value = user?.displayName ?: "—")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                    color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
                ProfileInfoRow(label = s.profileEmailLabel,  value = user?.email ?: "—")
                user?.createdAt?.let { ts ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.sm),
                        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                    ProfileInfoRow(label = s.profileMemberSince, value = formatProfileDate(ts))
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // ── Language section ──────────────────────────────────────────────────
        ProfileChoiceSection(
            title   = s.profileLanguageSection,
            options = AppLanguage.entries.map { it to it.displayName },
            current = currentLanguage,
            onSelect = LanguageManager::setLanguage,
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // ── Appearance section ────────────────────────────────────────────────
        ProfileChoiceSection(
            title    = s.profileAppearanceSection,
            options  = listOf(ThemeMode.SYSTEM to s.themeSystem, ThemeMode.LIGHT to s.themeLight, ThemeMode.DARK to s.themeDark),
            current  = currentTheme,
            onSelect = ThemeManager::setThemeMode,
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // ── Danger zone ───────────────────────────────────────────────────────
        ProfileSectionHeader(title = s.profileDangerZone)
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.xl),
            shape  = AppShape.xl,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            ProfileRowItem(
                icon    = Icons.AutoMirrored.Filled.ExitToApp,
                label   = s.profileSignOut,
                onClick = { showLogoutDialog = true },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            ProfileRowItem(
                icon      = Icons.Default.DeleteForever,
                label     = s.profileDeleteAccount,
                tint      = MaterialTheme.colorScheme.error,
                labelColor = MaterialTheme.colorScheme.error,
                onClick   = { showDeleteDialog = true },
                showArrow = false,
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.xxxl))
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(s.profileSignOutTitle) },
            text  = { Text(s.profileSignOutMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) { Text(s.profileSignOut) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(s.generalCancel) }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(s.profileDeleteTitle) },
            text  = { Text(s.profileDeleteMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(s.profileDeleteConfirm, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(s.generalCancel) }
            },
        )
    }

    if (showReAuthDialog) {
        AlertDialog(
            onDismissRequest = {
                showReAuthDialog = false
                authViewModel.cancelDeletion()
            },
            title = { Text(s.profileReAuthTitle) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(s.profileReAuthMessage)
                    AuthTextField(
                        value            = reAuthPassword,
                        onValueChange    = { reAuthPassword = it },
                        label            = s.profilePasswordLabel,
                        leadingIcon      = Icons.Default.Lock,
                        isPassword       = true,
                        showPassword     = showReAuthPassword,
                        onTogglePassword = { showReAuthPassword = !showReAuthPassword },
                    )
                    authState.error?.let { err ->
                        Text(
                            text  = err.resolve(s),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { authViewModel.confirmDeleteWithPassword(reAuthPassword) },
                    enabled = !authState.isLoading,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(s.profileDeleteButton, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReAuthDialog = false
                    authViewModel.cancelDeletion()
                }) { Text(s.generalCancel) }
            },
        )
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text     = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color    = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp,
        )
        Text(
            text     = value,
            fontSize = 14.sp,
            color    = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier  = modifier,
        shape     = AppShape.md,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(
                text       = value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary,
            )
            Text(
                text     = label,
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> ProfileChoiceSection(
    title: String,
    options: List<Pair<T, String>>,
    current: T,
    onSelect: (T) -> Unit,
) {
    ProfileSectionHeader(title = title)
    ElevatedCard(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xl),
        shape     = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            options.forEach { (value, label) ->
                val isSelected = current == value
                FilterChip(
                    selected = isSelected,
                    onClick  = { onSelect(value) },
                    label    = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text     = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start  = AppSpacing.xl + AppSpacing.lg,
            bottom = AppSpacing.xs,
            top    = AppSpacing.sm,
        ),
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun ProfileRowItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    showArrow: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint     = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text     = label,
            fontSize = 15.sp,
            color    = labelColor,
            modifier = Modifier.weight(1f),
        )
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatProfileDate(timestamp: Long): String {
    if (timestamp == 0L) return "—"
    val instant     = Instant.fromEpochMilliseconds(timestamp)
    val dateTime    = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val monthAbbrev = dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$monthAbbrev ${dateTime.year}"
}
