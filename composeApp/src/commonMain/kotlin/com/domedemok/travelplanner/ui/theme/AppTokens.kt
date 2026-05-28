package com.domedemok.travelplanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Spacing ───────────────────────────────────────────────────────────────────
object AppSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
}

// ── Shape ─────────────────────────────────────────────────────────────────────
object AppShape {
    val sm          = RoundedCornerShape(12.dp)
    val md          = RoundedCornerShape(16.dp)
    val lg          = RoundedCornerShape(20.dp)
    val xl          = RoundedCornerShape(24.dp)
    val xxl         = RoundedCornerShape(28.dp)
    val pill        = RoundedCornerShape(50)
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val bottomCard  = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
}

// ── Gradients ─────────────────────────────────────────────────────────────────
object AppGradients {

    // Primary action button — elegant slate sweep
    val primaryButton = Brush.linearGradient(
        colors = listOf(Slate700, Slate600)
    )

    // Profile header — deep slate sweep
    val profileHeader = Brush.linearGradient(
        colors = listOf(
            Color(0xFF21303E),   // very deep slate
            Slate700,            // 0xFF34495E
            Color(0xFF425A70),   // slightly brighter endpoint
        )
    )

    // AI chat header — integrated into the main theme using slate tones
    val aiChatHeader = Brush.linearGradient(
        colors = listOf(Color(0xFF2C3E50), Slate500)
    )

    // Photo overlay — neutral dark, works perfectly on all images
    val tripCardOverlay = Brush.verticalGradient(
        colors = listOf(Color(0x1A000000), Color(0xCC000000))
    )

    // ── Destination "vibe" chips ──────────────────────────────────────────────
    // Category-specific colours — slightly muted for elegance, but still distinct.
    val continuePlanning = Brush.linearGradient(listOf(Slate500, Slate700))

    val vibeBeach    = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
    val vibeMountain = Brush.linearGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A)))
    val vibeCulture  = Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFF7C3AED)))
    val vibeFoodie   = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)))
}

// ── Elevation ─────────────────────────────────────────────────────────────────
// Cards use 2 dp — a subtle physical lift. In dark mode Material3 adds a
// primary-tinted surface overlay automatically at this elevation.
object AppElevation {
    val none  = 0.dp
    val card  = 2.dp    // subtle shadow on ElevatedCards
    val modal = 8.dp
}