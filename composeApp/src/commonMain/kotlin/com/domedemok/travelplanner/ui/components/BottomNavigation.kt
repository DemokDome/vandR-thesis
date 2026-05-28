package com.domedemok.travelplanner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Place

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.domedemok.travelplanner.i18n.LocalStrings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.ui.theme.AppShape
import com.domedemok.travelplanner.ui.theme.AppSpacing

// ── Tab spec interface ────────────────────────────────────────────────────────

/** Minimal contract that both [MainTab] and [TripTab] satisfy. */
interface NavTabSpec {
    val activeIcon:   ImageVector
    val inactiveIcon: ImageVector
}

// ── Tab enums ─────────────────────────────────────────────────────────────────

enum class MainTab(
    val label: String,
    override val activeIcon: ImageVector,
    override val inactiveIcon: ImageVector,
) : NavTabSpec {
    EXPLORE(
        label       = "Discover",
        activeIcon   = Icons.Filled.Explore,
        inactiveIcon = Icons.Outlined.Explore,
    ),
    TRIPS(
        label       = "My Trips",
        activeIcon   = Icons.Filled.Map,
        inactiveIcon = Icons.Outlined.Map,
    ),
    INBOX(
        label       = "Inbox",
        activeIcon   = Icons.Filled.Notifications,
        inactiveIcon = Icons.Outlined.Notifications,
    ),
    PROFILE(
        label       = "Profile",
        activeIcon   = Icons.Filled.Person,
        inactiveIcon = Icons.Outlined.Person,
    ),
}

@Composable
fun MainTab.localizedLabel(): String {
    val s = LocalStrings.current
    return when (this) {
        MainTab.EXPLORE -> s.navDiscover
        MainTab.TRIPS   -> s.navMyTrips
        MainTab.INBOX   -> s.navInbox
        MainTab.PROFILE -> s.navProfile
    }
}

enum class TripTab(
    val label: String,
    override val activeIcon: ImageVector,
    override val inactiveIcon: ImageVector,
) : NavTabSpec {
    PLACES(
        label        = "Places",
        activeIcon   = Icons.Filled.Place,
        inactiveIcon = Icons.Outlined.Place,
    ),
    ITINERARY(
        label        = "Plan",
        activeIcon   = Icons.Filled.CalendarMonth,
        inactiveIcon = Icons.Outlined.CalendarMonth,
    ),
    MAP(
        label        = "Map",
        activeIcon   = Icons.Filled.Map,
        inactiveIcon = Icons.Outlined.Map,
    ),
    BUDGET(
        label        = "Budget",
        activeIcon   = Icons.Filled.CreditCard,
        inactiveIcon = Icons.Outlined.CreditCard,
    ),
    CHAT(
        label        = "Chat",
        activeIcon   = Icons.AutoMirrored.Filled.Chat,
        inactiveIcon = Icons.AutoMirrored.Outlined.Chat,
    ),
    GALLERY(
        label        = "Gallery",
        activeIcon   = Icons.Filled.PhotoLibrary,
        inactiveIcon = Icons.Outlined.PhotoLibrary,
    ),
}

@Composable
fun TripTab.localizedLabel(): String {
    val s = LocalStrings.current
    return when (this) {
        TripTab.PLACES    -> s.navPlaces
        TripTab.MAP       -> s.navMap
        TripTab.CHAT      -> s.navChat
        TripTab.BUDGET    -> s.navBudget
        TripTab.ITINERARY -> s.navPlan
        TripTab.GALLERY   -> s.navGallery
    }
}

// ── Shared nav item composable ─────────────────────────────────────────────────

@Composable
private fun NavItem(
    label: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "navIconColor",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 56.dp else 0.dp,
        animationSpec = tween(250),
        label = "navPillWidth",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .width(pillWidth.coerceAtLeast(40.dp))
                .height(28.dp)
                .clip(AppShape.pill)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else androidx.compose.ui.graphics.Color.Transparent
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isSelected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = iconColor,
            letterSpacing = 0.3.sp,
        )
    }
}

// ── Generic bottom nav ────────────────────────────────────────────────────────

/**
 * Shared bottom-navigation bar used by both [MainBottomNav] and [TripBottomNav].
 *
 * [items] must implement [NavTabSpec] so the bar can access [activeIcon] and
 * [inactiveIcon]. The locale-aware [label] lambda delegates to each tab's
 * existing `@Composable localizedLabel()` extension, keeping localisation
 * out of this layer entirely.
 */
@Composable
fun <T : NavTabSpec> BottomNav(
    items:          List<T>,
    selectedItem:   T,
    onItemSelected: (T) -> Unit,
    label:          @Composable (T) -> String,
    modifier:       Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            // 64.dp was exactly the sum of the inner content (8+28+4+16) → text
            // descenders (g, p, y) had no room and got clipped. 72.dp gives ~8.dp
            // of slack while still being more compact than Material's 80.dp default.
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        items.forEach { tab ->
            NavItem(
                label        = label(tab),
                activeIcon   = tab.activeIcon,
                inactiveIcon = tab.inactiveIcon,
                isSelected   = selectedItem == tab,
                onClick      = { onItemSelected(tab) },
                modifier     = Modifier.weight(1f),
            )
        }
    }
}

// ── Public convenience wrappers ───────────────────────────────────────────────

@Composable
fun MainBottomNav(
    selectedTab:   MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier:      Modifier = Modifier,
) = BottomNav(
    items          = MainTab.entries,
    selectedItem   = selectedTab,
    onItemSelected = onTabSelected,
    label          = { it.localizedLabel() },
    modifier       = modifier,
)

@Composable
fun TripBottomNav(
    selectedTab:   TripTab,
    onTabSelected: (TripTab) -> Unit,
    modifier:      Modifier = Modifier,
) = BottomNav(
    items          = TripTab.entries,
    selectedItem   = selectedTab,
    onItemSelected = onTabSelected,
    label          = { it.localizedLabel() },
    modifier       = modifier,
)
