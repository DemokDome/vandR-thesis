package com.domedemok.travelplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Entry-splash screen.
 *  - Branded hero with animated icon, app name and destination pills.
 *  - Two CTAs — primary "Get Started" (→ sign-up) and outline "Log In" (→ login).
 *  - If the user is already authenticated when this screen mounts, the parent
 *    nav stack redirects them away automatically (see App.kt LaunchedEffect).
 */
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
) {
    val s = LocalStrings.current
    // Delay the CTA reveal for a moment so the brand mark has time to breathe.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120)
        visible = true
    }

    // Subtle pulse on the brand mark.
    val pulse by rememberInfiniteTransition(label = "splashPulse").animateFloat(
        initialValue = 0.97f,
        targetValue  = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splashPulseValue",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepSlate,                  // 0xFF060F0E
                        DarkSurface,              // 0xFF0C1C1B
                        Color(0xFF091615),        // mid-tone
                    ),
                ),
            ),
    ) {
        // Ambient glow blobs — teal top-left, gold bottom-right.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-80).dp)
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x454CE8D8), Color.Transparent),  // teal glow
                    ),
                    shape = RoundedCornerShape(50),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x38F2C860), Color.Transparent),  // gold glow
                    ),
                    shape = RoundedCornerShape(50),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.xl),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.xxxl))

            // ── Hero brand block ───────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(500)) + slideInVertically(tween(600)) { it / 6 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // Gradient icon mark with pulse.
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = pulse
                                    scaleY = pulse
                                }
                                .size(88.dp)
                                .clip(AppShape.xl)
                                .background(AppGradients.primaryButton),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(44.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.lg))

                        Text(
                            text          = s.splashAppName,
                            color         = Color.White,
                            fontSize      = 40.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Text(
                            text       = s.splashTagline,
                            color      = Color.White.copy(alpha = 0.65f),
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.xxl))

                        // Floating destination pills.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        ) {
                            DestinationPill(s.splashParis)
                            DestinationPill(s.splashTokyo)
                            DestinationPill(s.splashNyc)
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        ) {
                            DestinationPill(s.splashRome)
                            DestinationPill(s.splashSantorini)
                        }
                    }
                }
            }

            // ── Bottom CTAs ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(700, delayMillis = 200)) +
                          slideInVertically(tween(700, delayMillis = 200)) { it / 3 },
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    GradientButton(
                        text    = s.splashGetStarted,
                        onClick = onGetStarted,
                    )

                    // Outline / glass button for the secondary action.
                    OutlinedButton(
                        onClick  = onLogIn,
                        shape    = AppShape.lg,
                        border   = null,
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor   = Color.White,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = AppShape.lg,
                            ),
                    ) {
                        Text(
                            text       = s.splashLogIn,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White.copy(alpha = 0.92f),
                        )
                    }

                    Text(
                        text       = s.splashTerms,
                        color      = Color.White.copy(alpha = 0.35f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier
                            .fillMaxWidth()
                            .padding(top = AppSpacing.xs),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xxl))
        }
    }
}

@Composable
private fun DestinationPill(text: String) {
    Surface(
        shape = AppShape.pill,
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.18f),
        ),
    ) {
        Text(
            text       = text,
            color      = Color.White.copy(alpha = 0.85f),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs + 2.dp),
        )
    }
}
