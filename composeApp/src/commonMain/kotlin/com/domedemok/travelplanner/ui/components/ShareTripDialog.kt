package com.domedemok.travelplanner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.util.JOIN_DEEP_LINK_PREFIX
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String) -> Unit,
    isLoading: Boolean = false,
    trip: Trip? = null,
    joinCode: String = "",               // resolved via TripDetailViewModel.ensureJoinCode()
) {
    val s         = LocalStrings.current
    var email     by remember { mutableStateOf("") }
    var copied    by remember { mutableStateOf(false) }
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    // Use the real joinCode if available; otherwise show a loading placeholder
    val displayCode = joinCode.ifBlank { "……" }
    val deepLink    = remember(joinCode) {
        if (joinCode.isNotBlank()) "$JOIN_DEEP_LINK_PREFIX$joinCode" else ""
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState       = sheetState,
        shape            = AppShape.bottomSheet,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        PopupThemeProvider {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Text(
                text       = trip?.let { s.shareTitle(it.name) } ?: s.shareFallback,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 20.sp,
            )

            Spacer(Modifier.height(AppSpacing.xs))

            // ── QR Code + join code block ─────────────────────────────────────
            Surface(
                shape = AppShape.lg,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier            = Modifier.padding(AppSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    // QR code
                    if (deepLink.isNotBlank()) {
                        val qrPainter = rememberQrCodePainter(deepLink)
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(AppShape.md)
                                .background(Color.White)
                                .padding(AppSpacing.sm),
                        ) {
                            Image(
                                painter            = qrPainter,
                                contentDescription = s.shareQrScan,
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier.fillMaxSize(),
                            )
                        }
                        Text(
                            text      = s.shareQrScan,
                            style     = MaterialTheme.typography.labelSmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        // Code is still loading
                        CircularProgressIndicator(
                            modifier    = Modifier.size(40.dp),
                            strokeWidth = 2.dp,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Join code display
                    Text(
                        text       = s.shareCodeLabel,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text       = displayCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 28.sp,
                        letterSpacing = 6.sp,
                        color      = MaterialTheme.colorScheme.onSurface,
                        textAlign  = TextAlign.Center,
                    )

                    // Copy code button
                    val copyBg = if (copied) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary
                    Surface(
                        shape    = AppShape.pill,
                        color    = copyBg,
                        enabled  = joinCode.isNotBlank(),
                        onClick  = {
                            clipboard.setText(AnnotatedString(joinCode))
                            copied = true
                        },
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        ) {
                            AnimatedContent(
                                targetState    = copied,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label          = "copyIcon",
                            ) { isCopied ->
                                Icon(
                                    imageVector        = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier.size(14.dp),
                                )
                            }
                            Text(
                                text       = if (copied) s.shareCopied else s.shareCopyCode,
                                color      = Color.White,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = AppSpacing.xs),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )

            // ── Email invite ──────────────────────────────────────────────────
            Text(
                text       = s.shareOrEmail,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                label         = { Text(s.shareEmailLabel) },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Done,
                ),
                shape = AppShape.lg,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                ),
                leadingIcon = {
                    Icon(
                        imageVector        = Icons.Default.Mail,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(20.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !isLoading,
            )

            Spacer(Modifier.height(AppSpacing.xs))

            // ── Send button ───────────────────────────────────────────────────
            Button(
                onClick  = { onConfirm(email) },
                enabled  = email.isNotBlank() && email.contains("@") && !isLoading,
                shape    = AppShape.lg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(AppSpacing.lg),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(s.shareSendInvite, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick   = onDismiss,
                enabled   = !isLoading,
                shape     = AppShape.lg,
                modifier  = Modifier.fillMaxWidth(),
            ) {
                Text(s.generalClose, fontWeight = FontWeight.SemiBold)
            }
        }
        }
    }
}
