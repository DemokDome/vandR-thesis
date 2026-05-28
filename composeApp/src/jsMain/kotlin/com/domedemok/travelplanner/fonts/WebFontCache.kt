package com.domedemok.travelplanner.fonts

/**
 * Holds font data loaded at startup for the Compose/Skia renderer.
 *
 * Skia/CanvasKit is completely isolated from the browser's CSS font system —
 * @font-face / document.fonts / <link rel="stylesheet"> have zero effect.
 * The only way to give Skia new typefaces is to pass raw ByteArrays to
 * Font(identity, data) before the first frame is rendered.
 *
 * ── Font files (place at composeApp/src/jsMain/resources/fonts/) ─────────────
 *
 *   PRIMARY (all weights of NotoSans — covers Latin/Extended, Cyrillic, Greek,
 *            Hebrew, Armenian, Georgian, and more in a single family):
 *     NotoSans-Regular.ttf        (~290 KB)
 *     NotoSans-Medium.ttf         (~295 KB)
 *     NotoSans-Bold.ttf           (~300 KB)
 *
 *   SCRIPT SUPPLEMENTS (Regular only — mainly needed for place names):
 *     NotoSansArabic-Regular.ttf  (~280 KB)  Arabic, Persian, Urdu
 *     NotoSansDevanagari-Regular.ttf (~300 KB)  Hindi, Marathi, Sanskrit
 *     NotoSansThai-Regular.ttf    (~180 KB)  Thai
 *
 *   CJK:
 *     NotoSansJP-Regular.ttf      (~8 MB)    Japanese + common CJK
 *
 *   EMOJI (must be LAST — see ordering note below):
 *     NotoColorEmoji.ttf          (~10 MB)
 *
 * ── Why NotoColorEmoji must be LAST ──────────────────────────────────────────
 * NotoColorEmoji includes glyphs for basic Latin characters (digits 0–9, space,
 * etc.) that it needs internally for keycap-emoji text sequences.  If it is
 * placed first in the FontFamily, Skia uses its wider space glyph and its
 * stylised digit glyphs for ALL text, causing wrong word-spacing and wrong
 * number appearance.  Placed last, it only receives characters that no
 * earlier font covers — i.e. actual emoji codepoints (U+1F300 and above).
 */
object WebFontCache {

    // Primary
    var notoSansBytes:         ByteArray? = null
    var notoSansMediumBytes:   ByteArray? = null
    var notoSansBoldBytes:     ByteArray? = null
    // Script supplements
    var notoArabicBytes:       ByteArray? = null
    var notoDevanagariBytes:   ByteArray? = null
    var notoThaiBytes:         ByteArray? = null
    // CJK
    var notoCjkBytes:          ByteArray? = null
    // Emoji — last
    var notoColorEmojiBytes:   ByteArray? = null

    // Paths are relative to the web root (= jsMain/resources/).
    // ORDER HERE DOES NOT MATTER — ordering in FallbackFonts.kt is what counts.
    private val fontSources = listOf(
        "notoSans"         to "fonts/NotoSans-Regular.ttf",
        "notoSansMedium"   to "fonts/NotoSans-Medium.ttf",
        "notoSansBold"     to "fonts/NotoSans-Bold.ttf",
        "notoArabic"       to "fonts/NotoSansArabic-Regular.ttf",
        "notoDevanagari"   to "fonts/NotoSansDevanagari-Regular.ttf",
        "notoThai"         to "fonts/NotoSansThai-Regular.ttf",
        "notoCjk"          to "fonts/NotoSansJP-Regular.ttf",
        "notoColorEmoji"   to "fonts/NotoColorEmoji.ttf",
    )

    /**
     * Loads all fonts concurrently; calls [onReady] once every request has
     * finished.  Missing files are silently skipped — the app starts regardless.
     */
    fun preload(onReady: () -> Unit) {
        var remaining = fontSources.size
        if (remaining == 0) { onReady(); return }

        fun tick() {
            remaining--
            if (remaining == 0) onReady()
        }

        fontSources.forEach { (key, path) ->
            fetchFontBytes(
                url       = path,
                onSuccess = { bytes ->
                    when (key) {
                        "notoSans"       -> notoSansBytes       = bytes
                        "notoSansMedium" -> notoSansMediumBytes = bytes
                        "notoSansBold"   -> notoSansBoldBytes   = bytes
                        "notoArabic"     -> notoArabicBytes     = bytes
                        "notoDevanagari" -> notoDevanagariBytes = bytes
                        "notoThai"       -> notoThaiBytes       = bytes
                        "notoCjk"        -> notoCjkBytes        = bytes
                        "notoColorEmoji" -> notoColorEmojiBytes = bytes
                    }
                    console.log("[WebFontCache] loaded: $key (${bytes.size / 1024} KB)")
                    tick()
                },
                onError = {
                    console.warn("[WebFontCache] failed to load: $path — skipping")
                    tick()
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Internal helper
// ---------------------------------------------------------------------------

private fun fetchFontBytes(
    url:       String,
    onSuccess: (ByteArray) -> Unit,
    onError:   () -> Unit,
) {
    try {
        val xhr: dynamic = js("new XMLHttpRequest()")
        xhr.open("GET", url, true)
        xhr.responseType = "arraybuffer"
        xhr.onload = {
            try {
                val status: Int = xhr.status.unsafeCast<Int>()
                if (status in 200..299) {
                    val bytes = js("new Int8Array(xhr.response)").unsafeCast<ByteArray>()
                    onSuccess(bytes)
                } else {
                    console.warn("[WebFontCache] HTTP $status for $url")
                    onError()
                }
            } catch (e: Throwable) {
                console.warn("[WebFontCache] parse error for $url: ${e.message}")
                onError()
            }
        }
        xhr.onerror   = { console.warn("[WebFontCache] network error: $url"); onError() }
        xhr.ontimeout = { console.warn("[WebFontCache] timeout: $url"); onError() }
        xhr.timeout   = 30_000
        xhr.send()
    } catch (e: Throwable) {
        console.warn("[WebFontCache] XHR setup failed for $url: ${e.message}")
        onError()
    }
}
