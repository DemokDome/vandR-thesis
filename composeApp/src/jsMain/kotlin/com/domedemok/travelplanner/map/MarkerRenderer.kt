package com.domedemok.travelplanner.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Place
import kotlin.math.abs

/**
 * Handles rendering of markers and popups on the map
 */
class MarkerRenderer(
    private val textMeasurer: TextMeasurer
) {

    companion object {
        // ⬅️ MARKER MÉRET - Itt tudod változtatni!
        private const val MARKER_SIZE = 30f          // Főméret (30px = kicsi, 40px = nagy)
        private const val MARKER_TIP_HEIGHT = 12f    // Alsó háromszög magasság
        private const val MARKER_RADIUS = MARKER_SIZE / 2f
        private const val HIT_RADIUS = MARKER_SIZE + 10f // Click terület (nagyobb a könnyebb kattintásért)
    }

    /**
     * Draw a single marker at the specified position.
     * [tint] is the base fill colour — defaults to the red accent.
     * When [isSelected] the colour darkens slightly.
     */
    fun DrawScope.drawMarker(
        position: Offset,
        isSelected: Boolean = false,
        tint: Color = Color(0xFFEF5350),
    ) {
        val fillColor = if (isSelected) tint.copy(
            red   = (tint.red   * 0.82f).coerceAtMost(1f),
            green = (tint.green * 0.82f).coerceAtMost(1f),
            blue  = (tint.blue  * 0.82f).coerceAtMost(1f),
        ) else tint

        // Main marker circle
        drawCircle(
            color  = fillColor,
            radius = MARKER_RADIUS,
            center = position,
            style  = Fill,
        )
        // White border
        drawCircle(
            color  = Color.White,
            radius = MARKER_RADIUS,
            center = position,
            style  = Stroke(width = 3f),
        )
        // Inner white dot
        drawCircle(
            color  = Color.White,
            radius = MARKER_SIZE * 0.2f,
            center = position,
            style  = Fill,
        )
        // Triangle tip pointing downward
        val tipWidth = MARKER_SIZE * 0.35f
        val tipPath = Path().apply {
            moveTo(position.x - tipWidth, position.y + MARKER_RADIUS - 4f)
            lineTo(position.x,            position.y + MARKER_RADIUS + MARKER_TIP_HEIGHT)
            lineTo(position.x + tipWidth, position.y + MARKER_RADIUS - 4f)
            close()
        }
        drawPath(path = tipPath, color = fillColor,      style = Fill)
        drawPath(path = tipPath, color = Color.White,    style = Stroke(width = 2f))
    }

    /**
     * Draw a popup for a selected place
     */
    /**
     * Draw a popup for a selected place
     */
    fun DrawScope.drawPopup(place: Place, position: Offset) {
        // Dinamikus, felbontásfüggetlen méretek kiszámítása
        val popupWidth = 220.dp.toPx()
        val popupPadding = 12.dp.toPx()
        val lineSpacing = 4.dp.toPx()
        val extraBlockSpacing = 8.dp.toPx()

        // Betűkhöz igazított dobozmagasságok
        val titleHeight = 22.sp.toPx()
        val normalHeight = 18.sp.toPx()
        val badgeHeight = 22.dp.toPx()

        // 1. Popup teljes magasságának kiszámítása
        var contentHeight = popupPadding * 2
        contentHeight += titleHeight // Name

        if (place.address.isNotEmpty()) {
            contentHeight += normalHeight + lineSpacing
        }
        if (place.category.isNotEmpty()) {
            contentHeight += badgeHeight + extraBlockSpacing + lineSpacing
        }
        if (place.notes.isNotEmpty()) {
            contentHeight += (normalHeight * 2) + extraBlockSpacing
        }

        val popupHeight = contentHeight

        // Pozicionálás
        val popupX = position.x - popupWidth / 2f
        val popupY = position.y - MARKER_SIZE - MARKER_TIP_HEIGHT - popupHeight - 10f

        // Árnyék
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(popupX + 4f, popupY + 4f),
            size = Size(popupWidth, popupHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )

        // Háttér
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(popupX, popupY),
            size = Size(popupWidth, popupHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )

        // Keret
        drawRoundRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(popupX, popupY),
            size = Size(popupWidth, popupHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            style = Stroke(width = 1f)
        )

        // Nyíl a marker felé
        val arrowPath = Path().apply {
            moveTo(position.x - 8f, popupY + popupHeight)
            lineTo(position.x, popupY + popupHeight + 8f)
            lineTo(position.x + 8f, popupY + popupHeight)
            close()
        }
        drawPath(arrowPath, Color.White, style = Fill)
        drawPath(arrowPath, Color(0xFFE0E0E0), style = Stroke(width = 1f))

        // --- SZÖVEGEK KIRAJZOLÁSA ---
        var textY = popupY + popupPadding

        // 1. Név (Bold)
        drawText(
            textMeasurer = textMeasurer,
            text = place.name,
            topLeft = Offset(popupX + popupPadding, textY),
            style = TextStyle(
                color = Color(0xFF1976D2),
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            size = Size(width = popupWidth - popupPadding * 2, height = titleHeight)
        )
        textY += titleHeight + lineSpacing

        // 2. Cím
        if (place.address.isNotEmpty()) {
            drawText(
                textMeasurer = textMeasurer,
                text = place.address,
                topLeft = Offset(popupX + popupPadding, textY),
                style = TextStyle(color = Color(0xFF666666), fontSize = 13.sp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                size = Size(width = popupWidth - popupPadding * 2, height = normalHeight)
            )
            textY += normalHeight + lineSpacing
        }

        // 3. Kategória (Badge)
        if (place.category.isNotEmpty()) {
            textY += extraBlockSpacing // Kis extra szünet a cím után

            val categoryFormatted = place.category.replace("_", " ")
            val badgeWidth = 90.dp.toPx()

            // Badge háttér
            drawRoundRect(
                color = Color(0xFFE3F2FD),
                topLeft = Offset(popupX + popupPadding, textY),
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Badge szöveg
            drawText(
                textMeasurer = textMeasurer,
                text = categoryFormatted,
                topLeft = Offset(popupX + popupPadding + 6.dp.toPx(), textY + 3.dp.toPx()),
                style = TextStyle(color = Color(0xFF1976D2), fontSize = 11.sp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                size = Size(width = badgeWidth - 12.dp.toPx(), height = normalHeight)
            )
            textY += badgeHeight + lineSpacing
        }

        // 4. Jegyzetek (Notes)
        if (place.notes.isNotEmpty()) {
            textY += extraBlockSpacing // Kis extra szünet a notes előtt

            drawText(
                textMeasurer = textMeasurer,
                text = place.notes,
                topLeft = Offset(popupX + popupPadding, textY),
                style = TextStyle(
                    color = Color(0xFF555555),
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                size = Size(width = popupWidth - popupPadding * 2, height = normalHeight * 2)
            )
        }
    }
    /**
     * Check if a click position hits a marker
     */
    fun isMarkerHit(clickPosition: Offset, markerPosition: Offset): Boolean {
        val dx = clickPosition.x - markerPosition.x
        val dy = clickPosition.y - markerPosition.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        return distance <= HIT_RADIUS
    }

    /**
     * Find which place (if any) was clicked
     */
    fun findClickedPlace(
        clickPosition: Offset,
        places: List<Place>,
        markerPositions: Map<String, Offset>
    ): Place? {
        // Check in reverse order so topmost markers are checked first
        for (place in places.reversed()) {
            val markerPos = markerPositions[place.id] ?: continue
            if (isMarkerHit(clickPosition, markerPos)) {
                return place
            }
        }
        return null
    }
}