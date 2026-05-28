package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.domedemok.travelplanner.ui.components.LayoutMode
import com.domedemok.travelplanner.ui.components.LocalIsOnline
import com.domedemok.travelplanner.ui.components.LocalLayoutMode
import com.domedemok.travelplanner.ui.components.MainBottomNav
import com.domedemok.travelplanner.ui.components.MainTab
import com.domedemok.travelplanner.ui.components.OfflineBanner
import com.domedemok.travelplanner.ui.components.WebMainSideNav

/**
 * Single nav-backstack entry that hosts the main tabs (Discover, My Trips, Inbox, Profile).
 *
 * **Compact window** (phones, foldables closed, narrow browsers): Scaffold with
 *   [MainBottomNav] at the bottom.
 * **Expanded window** (tablets in landscape, desktop browsers): persistent left
 *   sidebar ([WebMainSideNav]) with a full-height content area to the right.
 *
 * The branch is driven by [LocalLayoutMode] (set once at the App root from a
 * window-width measurement) — NOT by platform — so an Android tablet in
 * landscape gets the sidebar just like the web does.
 *
 * [rememberSaveableStateHolder] preserves each tab's Compose state (scroll
 * positions, form state, etc.) across tab switches AND across the
 * compact↔expanded transition when the window is resized.
 */
@Composable
fun MainShell(
    onTripClick: (String) -> Unit,
    onJoinTrip:  () -> Unit = {},
    onLogout:    () -> Unit,
) {
    val isExpandedLayout = LocalLayoutMode.current == LayoutMode.EXPANDED

    // rememberSaveable so the selected tab survives process death / config changes.
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.EXPLORE) }

    // Preserves each tab's subtree state across tab switches without resetting.
    val stateHolder = rememberSaveableStateHolder()

    if (isExpandedLayout) {
        // ── Expanded layout: sidebar + scrollable content area ────────────────
        Row(modifier = Modifier.fillMaxSize()) {
            WebMainSideNav(
                selectedTab   = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier      = Modifier.fillMaxHeight(),
            )

            VerticalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    OfflineBanner(isOnline = LocalIsOnline.current)

                    Box(modifier = Modifier.weight(1f)) {
                        stateHolder.SaveableStateProvider(key = selectedTab) {
                            when (selectedTab) {
                                MainTab.EXPLORE  -> ExploreScreen(onTripClick = onTripClick)
                                MainTab.TRIPS    -> TripListScreen(
                                    onTripClick = onTripClick,
                                    onJoinTrip  = onJoinTrip,
                                )
                                MainTab.INBOX    -> InboxScreen(onTripClick = onTripClick)
                                MainTab.PROFILE  -> ProfileScreen(onLogout = onLogout)
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ── Compact layout: Scaffold with bottom navigation ───────────────────
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar         = { OfflineBanner(isOnline = LocalIsOnline.current) },
            bottomBar      = {
                MainBottomNav(
                    selectedTab   = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                stateHolder.SaveableStateProvider(key = selectedTab) {
                    when (selectedTab) {
                        MainTab.EXPLORE  -> ExploreScreen(onTripClick = onTripClick)
                        MainTab.TRIPS    -> TripListScreen(
                            onTripClick = onTripClick,
                            onJoinTrip  = onJoinTrip,
                        )
                        MainTab.INBOX    -> InboxScreen(onTripClick = onTripClick)
                        MainTab.PROFILE  -> ProfileScreen(onLogout = onLogout)
                    }
                }
            }
        }
    }
}
