package com.domedemok.travelplanner

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.window.ComposeViewport
import com.domedemok.travelplanner.config.FirebaseConfig
import com.domedemok.travelplanner.di.appModule
import com.domedemok.travelplanner.fonts.WebFontCache
import com.domedemok.travelplanner.ui.components.LocalIsWeb
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initializeFirebase()

    startKoin {
        modules(appModule)
    }

    // Fetch TTF font files and store their raw bytes BEFORE ComposeViewport
    // initialises its Skia font manager.  Skia (CanvasKit) is completely isolated
    // from the browser's CSS font system, so <link> / document.fonts / @font-face
    // have zero effect on it.  The only way to give Skia new fonts is to pass
    // ByteArrays to Font(identity, data) before the first frame is rendered.
    WebFontCache.preload {
        // ComposeViewport (CMP 1.7+) automatically accounts for window.devicePixelRatio
        // when sizing the Skia canvas, so the rendering is already HiDPI-correct.
        //
        // LocalIsWeb = true marks this as the web platform. Layout decisions
        // (sidebar vs bottom nav) are NOT keyed off this flag — they read
        // LocalLayoutMode, which the App root derives from actual window width.
        // LocalIsWeb stays here for platform-bound capability checks.
        ComposeViewport(viewportContainerId = "root") {
            CompositionLocalProvider(
                LocalIsWeb      provides true,
                // Suppress the hover-state white-box that M3's default ripple produces on
                // the Skia/CanvasKit web canvas.  On web, pointer-enter events trigger the
                // hover state-layer which renders as a visible white rectangle over any
                // focusable component (most noticeably OutlinedTextField) because the
                // state layer draws against the transparent Skia canvas layer.
                // This replacement shows only a subtle press-flash so interactive elements
                // stay responsive without the hover artefact.
                LocalIndication provides WebPressOnlyIndication,
            ) {
                App()
            }
        }
    }
}

// ── Web-specific indication: press-flash only, no hover overlay ───────────────

private object WebPressOnlyIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return WebPressIndicationNode(interactionSource)
    }

    // Required by IndicationNodeFactory to optimize recomposition
    override fun equals(other: Any?): Boolean = other === this

    // Required by IndicationNodeFactory to optimize recomposition
    override fun hashCode(): Int = this::class.hashCode()

}

private class WebPressIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    // We use a simple mutableStateOf instead of collectIsPressedAsState()
    // because we are operating entirely outside of the Composition phase.
    private val isPressed = mutableStateOf(false)

    // onAttach is called when the modifier node is added to the UI tree.
    // The node provides a built-in `coroutineScope` tied to its lifecycle.
    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> isPressed.value = true
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> isPressed.value = false
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        // Reading isPressed.value inside draw() automatically creates a snapshot
        // observation. When the value changes, only the draw phase is invalidated,
        // avoiding costly recompositions.
        if (isPressed.value) {
            drawRect(Color(0x18FFFFFF))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

private fun initializeFirebase() {
    try {
        Firebase.initialize(
            options = FirebaseOptions(
                applicationId = FirebaseConfig.APP_ID,
                apiKey        = FirebaseConfig.API_KEY,
                databaseUrl   = FirebaseConfig.DATABASE_URL,
                authDomain    = FirebaseConfig.AUTH_DOMAIN,
                storageBucket = FirebaseConfig.STORAGE_BUCKET,
                projectId     = FirebaseConfig.PROJECT_ID,
                gcmSenderId   = FirebaseConfig.GCM_SENDER_ID,
            )
        )
        console.log("Firebase initialized successfully")
    } catch (e: Exception) {
        console.error("Firebase initialization failed: ${e.message}")
    }
}
