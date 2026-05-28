package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.InboxItem
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.components.EmojiText
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.util.getCurrentTimestamp
import com.domedemok.travelplanner.viewmodel.InboxViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onTripClick: (String) -> Unit = {},
    viewModel: InboxViewModel = koinViewModel(),
) {
    val s = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title            = { Text(s.inboxClearAllTitle) },
            text             = { Text(s.inboxClearAllBody) },
            confirmButton    = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(s.inboxClearAllConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text(s.generalCancel) }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(AppSpacing.xxxl),
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
                                imageVector        = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier           = Modifier.size(40.dp),
                                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Text(
                        text       = s.inboxEmpty,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text      = s.inboxEmptyBody,
                        fontSize  = 14.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start  = AppSpacing.lg,
                        end    = AppSpacing.lg,
                        top    = AppSpacing.lg,
                        bottom = AppSpacing.xxxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = AppSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(
                                text       = s.inboxTitle,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.onBackground,
                            )
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            ) {
                                Surface(
                                    shape = AppShape.pill,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text     = "${uiState.items.size}",
                                        modifier = Modifier.padding(
                                            horizontal = AppSpacing.md,
                                            vertical   = AppSpacing.xs,
                                        ),
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                IconButton(onClick = { showClearAllDialog = true }) {
                                    Icon(
                                        imageVector        = Icons.Default.DeleteSweep,
                                        contentDescription = s.inboxClearAllTitle,
                                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.items, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { it != SwipeToDismissBoxValue.Settled },
                        )
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                viewModel.dismissItem(item.id)
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = AppShape.lg,
                                        )
                                        .padding(horizontal = AppSpacing.lg),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint               = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            },
                        ) {
                            InboxCard(item = item, onTripClick = onTripClick)
                        }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AppSpacing.lg),
                action = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(LocalStrings.current.generalDismiss)
                    }
                },
            ) { Text(error) }
        }
    }
}

// ── Notification card ─────────────────────────────────────────────────────────

@Composable
private fun InboxCard(
    item: InboxItem,
    onTripClick: (String) -> Unit,
) {
    val s = LocalStrings.current

    val (icon, iconBg, iconTint, title, body) = when (item) {
        is InboxItem.ChatNotification -> ItemStyle(
            icon     = Icons.AutoMirrored.Filled.Chat,
            iconBg   = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            title    = s.inboxNewMessage(item.senderName),
            body     = item.messagePreview,
        )
        is InboxItem.PlaceNotification -> if (item.isDeleted) ItemStyle(
            icon     = Icons.Default.LocationOff,
            iconBg   = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
            title    = s.inboxPlaceRemoved(item.placeName),
            body     = s.inboxInTrip(item.tripName),
        ) else ItemStyle(
            icon     = Icons.Default.Place,
            iconBg   = Green600.copy(alpha = 0.15f),
            iconTint = Green700,
            title    = s.inboxNewPlace(item.placeName),
            body     = item.category.ifEmpty { s.inboxInTrip(item.tripName) },
        )
        is InboxItem.ExpenseNotification -> if (item.isDeleted) ItemStyle(
            icon     = Icons.Default.MoneyOff,
            iconBg   = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
            title    = s.inboxExpenseRemoved(item.title),
            body     = s.inboxInTrip(item.tripName),
        ) else ItemStyle(
            icon     = Icons.Default.CreditCard,
            iconBg   = Amber500.copy(alpha = 0.15f),
            iconTint = Amber600,
            title    = s.inboxNewExpense(item.title),
            body     = "${item.paidByName}  ·  ${budgetFormatAmount(item.amount)}",
        )
        is InboxItem.MemberNotification -> when {
            item.isSelf && item.isJoined -> ItemStyle(
                icon     = Icons.Default.GroupAdd,
                iconBg   = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                title    = s.inboxAddedToTrip(item.tripName),
                body     = s.inboxInTrip(item.tripName),
            )
            item.isJoined -> ItemStyle(
                icon     = Icons.Default.PersonAdd,
                iconBg   = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title    = s.inboxMemberJoined(item.memberName),
                body     = s.inboxInTrip(item.tripName),
            )
            else -> ItemStyle(
                icon     = Icons.Default.PersonRemove,
                iconBg   = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                title    = s.inboxMemberLeft(item.memberName),
                body     = s.inboxInTrip(item.tripName),
            )
        }
    }

    ElevatedCard(
        onClick   = { onTripClick(item.tripId) },
        shape     = AppShape.lg,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            // Icon bubble
            Surface(
                shape    = AppShape.md,
                color    = iconBg,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = iconTint,
                        modifier           = Modifier.size(22.dp),
                    )
                }
            }

            // Text content — EmojiText so that place/trip names, category labels,
            // and chat messages render correctly on JS/CanvasKit (NotoColorEmoji base font).
            Column(modifier = Modifier.weight(1f)) {
                EmojiText(
                    text       = title,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if ((item is InboxItem.PlaceNotification && item.isDeleted) ||
                                     (item is InboxItem.ExpenseNotification && item.isDeleted))
                                     MaterialTheme.colorScheme.onSurfaceVariant
                                 else MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                EmojiText(
                    text     = body,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                EmojiText(
                    text     = s.inboxInTrip(item.tripName),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Relative timestamp
            Text(
                text     = relativeTime(item.timestamp, s),
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

// ── Helper: destructured display data for each item type ──────────────────────

private data class ItemStyle(
    val icon:     ImageVector,
    val iconBg:   Color,
    val iconTint: Color,
    val title:    String,
    val body:     String,
)

// ── Relative timestamp ────────────────────────────────────────────────────────

private fun relativeTime(timestamp: Long, s: com.domedemok.travelplanner.i18n.Strings): String {
    val diffMs  = getCurrentTimestamp() - timestamp
    val diffMin = (diffMs / 60_000).toInt()
    val diffHr  = (diffMs / 3_600_000).toInt()
    val diffDay = (diffMs / 86_400_000).toInt()
    return when {
        diffMin < 1  -> s.inboxJustNow
        diffMin < 60 -> s.inboxMinutesAgo(diffMin)
        diffHr  < 24 -> s.inboxHoursAgo(diffHr)
        diffDay == 1 -> s.inboxYesterday
        else         -> s.inboxDaysAgo(diffDay)
    }
}
