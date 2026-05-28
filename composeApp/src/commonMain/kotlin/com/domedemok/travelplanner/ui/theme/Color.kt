package com.domedemok.travelplanner.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════════════════
//  TravelPlanner — "Aegean Slate × Warm Sand" design system
//
//  Concept
//  ───────
//  Light  "Clear Horizon" — clean off-white backgrounds; a sophisticated deep
//         slate blue primary evokes reliability and the ocean; a muted, warm
//         sand/terracotta secondary brings in an earthy, premium feel.
//
//  Dark   "Night Flight" — elegant, deep slate-grey backgrounds; soft
//         pastel blue primary ensures readability without vibrating; muted
//         sand secondary provides warm, comfortable contrast.
//
//  The palette is highly cohesive, low-vibration, and elegant. The AI chat
//  is now fully integrated into the core theme without using separate colors.
// ════════════════════════════════════════════════════════════════════════════

// ── Dark theme — Night Flight ────────────────────────────────────────────────
val dark_primary              = Color(0xFF9CB4CC)   // soft slate blue — readable, calm
val dark_onPrimary            = Color(0xFF041A2D)   // deep navy
val dark_primaryContainer     = Color(0xFF1D3145)   // mid-dark slate container
val dark_onPrimaryContainer   = Color(0xFFD6E4F0)   // light blue text
val dark_secondary            = Color(0xFFD6A68A)   // soft warm sand
val dark_onSecondary          = Color(0xFF2B1202)   // deep terracotta/brown
val dark_secondaryContainer   = Color(0xFF4A2814)   // dark terracotta container
val dark_onSecondaryContainer = Color(0xFFF3DFD1)   // pale warm cream text
val dark_background           = Color(0xFF12161A)   // elegant deep slate-grey
val dark_onBackground         = Color(0xFFDFE3E7)   // cool light grey
val dark_surface              = Color(0xFF191E24)   // raised surface
val dark_onSurface            = Color(0xFFDFE3E7)   // cool light grey
val dark_surfaceVariant       = Color(0xFF3C444B)   // input / chip background
val dark_onSurfaceVariant     = Color(0xFFC0C7CD)   // muted grey for secondary text
val dark_outline              = Color(0xFF8B959E)   // subtle border
val dark_error                = Color(0xFFE29A9A)   // soft, muted red
val dark_onError              = Color(0xFF4A0A0A)
val dark_errorContainer       = Color(0xFF6B1C1C)
val dark_onErrorContainer     = Color(0xFFF5C6C6)
val dark_inverseSurface       = Color(0xFFDFE3E7)
val dark_inverseOnSurface     = Color(0xFF191E24)

// ── Light theme — Clear Horizon ──────────────────────────────────────────────
val light_primary              = Color(0xFF34495E)   // deep slate blue — elegant, premium
val light_onPrimary            = Color(0xFFFFFFFF)
val light_primaryContainer     = Color(0xFFD6E4F0)   // soft pale blue — fresh
val light_onPrimaryContainer   = Color(0xFF0B1D2E)   // very deep navy for contrast
val light_secondary            = Color(0xFFAF7A5D)   // muted terracotta / warm sand
val light_onSecondary          = Color(0xFFFFFFFF)
val light_secondaryContainer   = Color(0xFFF3DFD1)   // pale warm sand
val light_onSecondaryContainer = Color(0xFF3B1C0B)   // very deep brown
val light_background           = Color(0xFFF7F9FA)   // highly clean, faint cool off-white
val light_onBackground         = Color(0xFF171C20)   // near black / deep slate
val light_surface              = Color(0xFFFFFFFF)   // pure white cards
val light_onSurface            = Color(0xFF171C20)   // deep slate for body text
val light_surfaceVariant       = Color(0xFFE3E8EC)   // faint grey-blue for inputs / chips
val light_onSurfaceVariant     = Color(0xFF464E54)   // muted dark grey for secondary text
val light_outline              = Color(0xFF778087)   // subtle neutral border
val light_error                = Color(0xFFBA3B3B)   // elegant, desaturated red
val light_onError              = Color(0xFFFFFFFF)
val light_errorContainer       = Color(0xFFFFDAD6)
val light_onErrorContainer     = Color(0xFF410002)
val light_inverseSurface       = Color(0xFF171C20)
val light_inverseOnSurface     = Color(0xFFF7F9FA)

// ── Fixed palette — referenced by name across the UI ────────────────────────

// Slate — primary family
val Slate300 = Color(0xFFB8D0E6)   // very light, for subtle tints
val Slate400 = Color(0xFF9CB4CC)   // dark-mode primary
val Slate500 = Color(0xFF6785A3)   // mid-range / chat accents
val Slate600 = Color(0xFF4A6581)   // button hover
val Slate700 = Color(0xFF34495E)   // light-mode primary

// Sand/Terracotta — secondary family
val Sand300 = Color(0xFFE2BC9F)   // subtle warm tints
val Sand400 = Color(0xFFD6A68A)   // dark-mode secondary
val Sand600 = Color(0xFFAF7A5D)   // light-mode secondary

// Semantic accents — stable, widely used with specific meaning
val Amber400 = Color(0xFFEAB308)   // favourites heart (filled) - less neon
val Amber500 = Color(0xFFD97706)   // ratings / highlights
val Amber600 = Color(0xFFB45309)   // edit-action icons
val Green400 = Color(0xFF4ADE80)   // positive indicators / upcoming chip dark-mode
val Green500 = Color(0xFF16A34A)   // budget surplus / split balanced
val Green600 = Color(0xFF22C55E)   // continuePlanning gradient start / medium accent
val Green700 = Color(0xFF15803D)   // continuePlanning gradient end / upcoming chip light-mode

// Semantic UI colors
val HeartRed = Color(0xFFEF4444)   // favorite heart (filled) — warm vibrant red

// Dark surface aliases (used in SplashScreen and overlays)
val DeepSlate    = Color(0xFF12161A)   // dark background  (matches dark_background)
val DarkSurface  = Color(0xFF191E24)   // dark surface     (matches dark_surface)
val DarkSurface2 = Color(0xFF232A32)   // dark surface variant

val OverlayDark  = Color(0x99000000)   // ~60 % — clean neutral black over photos
val OverlayLight = Color(0x40000000)   // ~25 % — lighter neutral photo overlay