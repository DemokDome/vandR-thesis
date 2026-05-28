package com.domedemok.travelplanner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.util.JOIN_CODE_LENGTH
import com.domedemok.travelplanner.util.cleanJoinCode
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLVideoElement

/**
 * Web implementation using the BarcodeDetector Web API (Chrome / Edge).
 *
 * Creates a full-screen DOM overlay on top of the Compose canvas, starts the
 * device camera, and scans frames every 500 ms. Falls back to a message if
 * the browser does not support BarcodeDetector.
 */
@Composable
actual fun QrScannerDialog(onCodeScanned: (String) -> Unit, onDismiss: () -> Unit) {

    // Snapshot localised strings before entering the effect — DOM textContent
    // mutation happens outside the composition and can't read CompositionLocals.
    val s                  = LocalStrings.current
    val hintText           = s.joinTripScanHint
    val cameraUnavailable  = s.joinTripCameraUnavailable
    val unsupportedText    = s.joinTripScannerNotSupported
    val permissionDenied   = s.joinTripCameraPermission
    val closeLabel         = "✕  ${s.generalClose}"

    DisposableEffect(Unit) {

        // ── Build DOM overlay ─────────────────────────────────────────────────
        val overlay = (document.createElement("div") as HTMLDivElement).apply {
            style.cssText = """
                position:fixed;top:0;left:0;width:100%;height:100%;
                background:rgba(0,0,0,0.92);z-index:99999;
                display:flex;flex-direction:column;align-items:center;
                justify-content:center;gap:20px;
                font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
            """.trimIndent()
        }

        val video = (document.createElement("video") as HTMLVideoElement).apply {
            autoplay = true
            setAttribute("playsinline", "true")
            style.cssText = """
                width:100%;max-width:420px;border-radius:16px;
                display:block;background:#111;
            """.trimIndent()
        }

        val hint = (document.createElement("p") as HTMLParagraphElement).apply {
            textContent = hintText
            style.cssText = "color:rgba(255,255,255,0.85);margin:0;font-size:15px;text-align:center;"
        }

        val closeBtn = (document.createElement("button") as HTMLButtonElement).apply {
            textContent = closeLabel
            style.cssText = """
                padding:12px 36px;background:#fff;border:none;
                border-radius:24px;font-size:16px;font-weight:600;
                cursor:pointer;color:#111;
            """.trimIndent()
        }
        closeBtn.addEventListener("click", { onDismiss() })

        overlay.appendChild(video)
        overlay.appendChild(hint)
        overlay.appendChild(closeBtn)
        document.body?.appendChild(overlay)

        // ── Camera + BarcodeDetector setup ────────────────────────────────────
        // Mutable JS state lives in a `dynamic` object to avoid Kotlin IR
        // variable-mangling issues inside js() blocks.
        val state: dynamic = js("({ intervalId: 0, stream: null })")

        val nav = window.navigator.asDynamic()
        if (nav.mediaDevices == null || nav.mediaDevices == undefined) {
            hint.textContent = cameraUnavailable
        } else {
            // getUserMedia — prefer back camera on mobile
            val constraints: dynamic = js("({ video: { facingMode: 'environment' } })")

            @Suppress("UNCHECKED_CAST")
            val getUserMedia = nav.mediaDevices.getUserMedia(constraints)
                .unsafeCast<kotlin.js.Promise<dynamic>>()

            getUserMedia.then(onFulfilled = { stream: dynamic ->
                state.stream = stream
                video.asDynamic().srcObject = stream

                // BarcodeDetector is available in Chrome 83+ / Edge 83+
                val hasBarcodeDetector: Boolean = js("'BarcodeDetector' in window")
                if (!hasBarcodeDetector) {
                    hint.textContent = unsupportedText
                } else {
                    val detector: dynamic = js("new window.BarcodeDetector({ formats: ['qr_code'] })")
                    state.intervalId = window.setInterval({
                        @Suppress("UNCHECKED_CAST")
                        val detectPromise = detector.detect(video)
                            .unsafeCast<kotlin.js.Promise<dynamic>>()
                        detectPromise.then(onFulfilled = { barcodes: dynamic ->
                            val len = (barcodes.length as Number).toInt()
                            if (len > 0) {
                                val raw = barcodes[0].rawValue as? String ?: ""
                                val code = cleanJoinCode(raw)

                                if (code.length == JOIN_CODE_LENGTH) {
                                    window.clearInterval(state.intervalId as Int)
                                    onCodeScanned(code)
                                }
                            }
                            null
                        })
                        null
                    }, 500)
                }
                null
            }, onRejected = { _: dynamic ->
                hint.textContent = permissionDenied
                null
            })
        }

        // ── Cleanup ───────────────────────────────────────────────────────────
        onDispose {
            val id = state.intervalId as? Int ?: 0
            if (id != 0) window.clearInterval(id)

            val stream = state.stream
            if (stream != null && stream != undefined) {
                @Suppress("UNCHECKED_CAST")
                (stream.getTracks() as? Array<dynamic>)?.forEach { it.stop() }
            }

            runCatching { document.body?.removeChild(overlay) }
        }
    }
}
