package com.domedemok.travelplanner.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout footprint of the host window — drives navigation choice (bottom bar
 * vs. side rail) and any other purely *spatial* layout decisions.
 *
 * Distinct from [LocalIsWeb] on purpose: that flag answers "which *platform*
 * are we on", this enum answers "how *wide* is the window". On a 10" Android
 * tablet in landscape both `LocalIsWeb == false` and `LocalLayoutMode == EXPANDED`
 * are correct — and the sidebar is the right UX even though we're not on web.
 */
enum class LayoutMode {
    /** Narrow window — phones, foldables closed, small browser windows. Use bottom navigation. */
    COMPACT,

    /** Wide window — tablets in landscape, desktop browsers. Use a persistent side navigation. */
    EXPANDED,
}

/**
 * Threshold above which we switch from bottom navigation to a side rail.
 *
 * Derivation:
 *  - The side rail itself is [com.domedemok.travelplanner.ui.components.SideNavWidth] (240.dp).
 *  - A comfortable minimum content area for our two-column trip grids and
 *    tables is ~520.dp.
 *  - 240 + ~16 (divider+padding) + 520 ≈ 776.dp absolute minimum.
 *
 * Rounded up to 840.dp to align with the Material 3 "Expanded" window-size
 * class — the official guideline breakpoint for switching navigation patterns.
 * Gives a ~64.dp buffer over the geometric minimum so the layout never feels
 * cramped right at the transition.
 *
 * Notable cases at this threshold:
 *  - Phone portrait (~411.dp) → COMPACT ✓
 *  - Phone landscape (~731.dp) → COMPACT ✓ (intentional: sidebar wastes width on phones)
 *  - Galaxy Fold inner display (~673.dp) → COMPACT ✓
 *  - 10" tablet landscape (~960.dp+) → EXPANDED ✓
 *  - Typical desktop browser → EXPANDED ✓
 */
val EXPANDED_LAYOUT_BREAKPOINT: Dp = 840.dp

/**
 * Resolves the current window's layout footprint.
 *
 * Provided once at the App root from a `BoxWithConstraints` measurement —
 * downstream screens just read this instead of doing their own size checks
 * (keeps the breakpoint and the flip semantics in exactly one place).
 *
 * Default of `COMPACT` is the safe fallback: if a screen is composed outside
 * the provider (tests, previews), it renders the phone layout rather than
 * crashing.
 */
val LocalLayoutMode = compositionLocalOf { LayoutMode.COMPACT }

/**
 * Pure mapping from measured window width to [LayoutMode]. Called every time
 * the host re-measures (e.g. browser resize), but downstream readers of
 * [LocalLayoutMode] are only invalidated when the *enum* value actually
 * changes — [compositionLocalOf] does a structural equality check internally,
 * so per-pixel resizes don't cascade through the tree.
 */
fun layoutModeFor(windowWidth: Dp): LayoutMode =
    if (windowWidth >= EXPANDED_LAYOUT_BREAKPOINT) LayoutMode.EXPANDED else LayoutMode.COMPACT
