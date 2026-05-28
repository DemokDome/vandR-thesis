package com.domedemok.travelplanner.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.*

private enum class ChatTab { GROUP, AI }

@Composable
fun TripChatWrapperScreen(tripId: String) {
    val s = LocalStrings.current
    var selectedTab by remember { mutableStateOf(ChatTab.GROUP) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {

        // ── Tab selector header ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppGradients.profileHeader)
                .padding(
                    start  = AppSpacing.xl,
                    end    = AppSpacing.xl,
                    top    = AppSpacing.sm,
                    bottom = AppSpacing.sm,
                ),
        ) {
            // Pill segmented control
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = AppShape.pill,
                color    = Color.White.copy(alpha = 0.15f),
            ) {
                Row(modifier = Modifier.padding(AppSpacing.xs)) {
                    ChatTabButton(
                        label      = s.chatTabGroup,
                        icon       = Icons.AutoMirrored.Filled.Chat,
                        isSelected = selectedTab == ChatTab.GROUP,
                        onClick    = { selectedTab = ChatTab.GROUP },
                        modifier   = Modifier.weight(1f),
                    )
                    ChatTabButton(
                        label      = s.chatTabAi,
                        icon       = Icons.Default.AutoAwesome,
                        isSelected = selectedTab == ChatTab.AI,
                        onClick    = { selectedTab = ChatTab.AI },
                        modifier   = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when (selectedTab) {
            ChatTab.GROUP -> ChatScreen(tripId = tripId, modifier = Modifier.fillMaxSize())
            ChatTab.AI    -> AiChatScreen(tripId = tripId, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ChatTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Selected: theme surface background with onSurface text — works in both
    // light (white pill, dark text) and dark (dark-teal pill, light text) modes.
    // Unselected: transparent pill, white text — intentionally white since the
    // header gradient is always violet regardless of theme.
    val bgColor by animateColorAsState(
        targetValue  = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(200),
        label        = "chatTabBg",
    )
    val contentColor by animateColorAsState(
        targetValue  = if (isSelected) MaterialTheme.colorScheme.onSurface
                       else           Color.White.copy(alpha = 0.8f),
        animationSpec = tween(200),
        label        = "chatTabContent",
    )

    Box(
        modifier = modifier
            .clip(AppShape.pill)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = contentColor,
                modifier           = Modifier.size(16.dp),
            )
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color      = contentColor,
            )
        }
    }
}
