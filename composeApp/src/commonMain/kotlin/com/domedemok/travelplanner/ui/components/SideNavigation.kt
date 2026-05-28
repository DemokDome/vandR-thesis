package com.domedemok.travelplanner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.AppShape
import com.domedemok.travelplanner.ui.theme.AppSpacing

// ── Constants ─────────────────────────────────────────────────────────────────

/** Fixed width of all side navigation panels on web. */
val SideNavWidth: Dp = 240.dp

// ── Main app sidebar (Discover / My Trips / Profile) ─────────────────────────

@Composable
fun WebMainSideNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier       = modifier.width(SideNavWidth).fillMaxHeight(),
        tonalElevation = 2.dp,
        color          = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App wordmark
            Text(
                text     = "vandR",
                style    = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    start  = AppSpacing.lg,
                    end    = AppSpacing.lg,
                    top    = AppSpacing.xxl,
                    bottom = AppSpacing.lg,
                ),
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppSpacing.md),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )

            Spacer(Modifier.height(AppSpacing.sm))

            MainTab.entries.forEach { tab ->
                SideNavItem(
                    label      = tab.localizedLabel(),
                    icon       = if (selectedTab == tab) tab.activeIcon else tab.inactiveIcon,
                    isSelected = selectedTab == tab,
                    onClick    = { onTabSelected(tab) },
                )
            }
        }
    }
}

// ── Trip sidebar (Places / Plan / Map / Budget / Chat / Gallery) ──────────────

@Composable
fun WebTripSideNav(
    selectedTab:     TripTab,
    onTabSelected:   (TripTab) -> Unit,
    onBack:          () -> Unit,
    trip:            Trip?,
    isOwner:         Boolean,
    isOnline:        Boolean,
    onManageMembers: () -> Unit,
    onEdit:          () -> Unit,
    onShare:         () -> Unit,
    /** Owner → delete trip; non-owner → leave trip. */
    onDeleteOrLeave: () -> Unit,
    modifier:        Modifier = Modifier,
) {
    val s = LocalStrings.current

    Surface(
        modifier       = modifier.width(SideNavWidth).fillMaxHeight(),
        tonalElevation = 2.dp,
        color          = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Back button ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = null,
                        indication        = null,
                        onClick           = onBack,
                    )
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s.generalBack,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(18.dp),
                )
                Text(
                    text  = s.generalBack,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Trip name ─────────────────────────────────────────────────────
            if (trip != null) {
                Text(
                    text       = trip.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines   = 2,
                    modifier   = Modifier.padding(
                        start  = AppSpacing.lg,
                        end    = AppSpacing.lg,
                        bottom = AppSpacing.sm,
                    ),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppSpacing.md),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )

            Spacer(Modifier.height(AppSpacing.sm))

            // ── Tab items ─────────────────────────────────────────────────────
            TripTab.entries.forEach { tab ->
                SideNavItem(
                    label      = tab.localizedLabel(),
                    icon       = if (selectedTab == tab) tab.activeIcon else tab.inactiveIcon,
                    isSelected = selectedTab == tab,
                    onClick    = { onTabSelected(tab) },
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Action buttons at bottom ──────────────────────────────────────
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppSpacing.md),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )

            if (trip != null) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // Inline action row — favourite is owned by TripListScreen,
                    // and the old "more" menu has been replaced with discrete
                    // edit / invite / delete-or-leave icons.
                    SideActionButton(
                        icon        = Icons.Default.People,
                        description = s.membersTitle,
                        onClick     = onManageMembers,
                    )
                    SideActionButton(
                        icon        = Icons.Default.Edit,
                        description = s.tripDetailEditLabel,
                        onClick     = onEdit,
                        enabled     = isOnline,
                    )
                    SideActionButton(
                        icon        = Icons.Default.PersonAdd,
                        description = s.tripDetailInviteLabel,
                        onClick     = onShare,
                        enabled     = isOnline,
                    )
                    SideActionButton(
                        icon        = if (isOwner) Icons.Default.DeleteOutline
                                      else Icons.AutoMirrored.Filled.ExitToApp,
                        description = if (isOwner) s.tripDetailDeleteLabel else s.tripDetailLeaveLabel,
                        onClick     = onDeleteOrLeave,
                        enabled     = isOnline,
                        tintColor   = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.md))
        }
    }
}

// ── Shared private composables ────────────────────────────────────────────────

@Composable
private fun SideNavItem(
    label:      String,
    icon:       ImageVector,
    isSelected: Boolean,
    onClick:    () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label         = "sideNavBg",
    )
    val contentColor by animateColorAsState(
        targetValue   = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label         = "sideNavFg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm, vertical = 2.dp)
            .clip(AppShape.lg)
            .background(bgColor)
            .clickable(
                interactionSource = null,
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = contentColor,
            modifier           = Modifier.size(22.dp),
        )
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = contentColor,
        )
    }
}

@Composable
private fun SideActionButton(
    icon:        ImageVector,
    description: String,
    onClick:     () -> Unit,
    enabled:     Boolean = true,
    tintColor:   androidx.compose.ui.graphics.Color? = null,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = tintColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(22.dp),
        )
    }
}
