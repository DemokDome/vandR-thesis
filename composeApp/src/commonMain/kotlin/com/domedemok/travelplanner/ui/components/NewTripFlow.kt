package com.domedemok.travelplanner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.components.EmojiText
import com.domedemok.travelplanner.ui.screens.GradientButton
import com.domedemok.travelplanner.ui.theme.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// ── Preset destinations ─────────────────────────────────────────────────────
// A curated list of popular cities; serves the Figma "inspiration chips".
// Picking a preset auto-fills the trip name + description on later steps.

private data class PresetDestination(
    val id: String,
    val name: String,
    val country: String,
    val emoji: String,
    val tagline: String,
)

private val presetDestinations = listOf(
    PresetDestination("paris",     "Paris",     "France",        "🗼", "City of Light & Romance"),
    PresetDestination("tokyo",     "Tokyo",     "Japan",         "⛩️", "Tradition Meets the Future"),
    PresetDestination("new-york",  "New York",  "United States", "🗽", "The City That Never Sleeps"),
    PresetDestination("rome",      "Rome",      "Italy",         "🏛️", "The Eternal City"),
    PresetDestination("santorini", "Santorini", "Greece",        "🌅", "Island of Blue Domes"),
    PresetDestination("barcelona", "Barcelona", "Spain",         "🎨", "Gaudí's Playground"),
    PresetDestination("bangkok",   "Bangkok",   "Thailand",      "🛕", "City of Angels"),
    PresetDestination("london",    "London",    "UK",            "🎡", "Crown Jewel of Europe"),
)

// ── Public entry point ──────────────────────────────────────────────────────

/**
 * Three-step trip creation wizard, ported from the Figma `NewTripFlow`.
 *
 * Steps:
 *   0. Name        — required trip name + optional description.
 *   1. Destination — picker grid of preset cities + free-text search.
 *   2. Dates       — custom month-grid date-range picker with optional Skip.
 *
 * [destination] is now a first-class field passed to [onCreate] separately
 * so it can be stored on the Trip model and auto-filled in the Places tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripFlow(
    onDismiss: () -> Unit,
    onCreate: (name: String, destination: String, description: String, startDate: Long, endDate: Long) -> Unit,
    isLoading: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState       = sheetState,
        shape            = AppShape.bottomSheet,
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
        PopupThemeProvider {
            NewTripFlowContent(
                onDismiss = onDismiss,
                onCreate  = onCreate,
                isLoading = isLoading,
            )
        }
    }
}

// ── Internal state machine ──────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
private fun NewTripFlowContent(
    onDismiss: () -> Unit,
    onCreate:  (name: String, destination: String, description: String, startDate: Long, endDate: Long) -> Unit,
    isLoading: Boolean,
) {
    val s = LocalStrings.current
    var step         by remember { mutableStateOf(0) }
    var direction    by remember { mutableStateOf(1) } // +1 forward, -1 back

    // Step 0 — Name
    var tripName     by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }

    // Step 1 — Destination
    var chosenPreset by remember { mutableStateOf<PresetDestination?>(null) }
    var freeText     by remember { mutableStateOf("") }

    // Step 2 — Dates
    var startDate    by remember { mutableStateOf<LocalDate?>(null) }
    var endDate      by remember { mutableStateOf<LocalDate?>(null) }

    // Destination validity — either a preset is chosen or the free-text field has a value.
    val destinationPicked: Boolean     = chosenPreset != null || freeText.isNotBlank()
    val destinationDisplayName: String = chosenPreset?.let { "${it.name}, ${it.country}" } ?: freeText.trim()

    val canAdvance = when (step) {
        0    -> tripName.isNotBlank()
        1    -> destinationPicked
        2    -> true  // dates are optional (Skip is surfaced inside the step)
        else -> false
    }

    val goNext = {
        direction = 1
        step      = (step + 1).coerceAtMost(2)
    }
    val goBack = {
        direction = -1
        step      = (step - 1).coerceAtLeast(0)
    }

    val submit = submit@{
        if (!canAdvance) return@submit
        val startMillis = startDate?.toEpochMillis() ?: 0L
        val endMillis   = endDate?.toEpochMillis()   ?: 0L

        onCreate(
            tripName.trim(),
            destinationDisplayName,
            description.trim(),
            startMillis,
            endMillis,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xl)
            .padding(bottom = AppSpacing.xl)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        // ── Top row: title + close ─────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = s.newTripEyebrow,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Black,
                    color      = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                )
                Text(
                    text       = s.newTripFlowTitle,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onDismiss, enabled = !isLoading) {
                Icon(Icons.Default.Close, contentDescription = s.generalClose)
            }
        }

        // ── Step dots ──────────────────────────────────────────────────────
        StepDots(step = step, total = 3)

        // ── Step content with horizontal slide animation ──────────────────
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = direction
                (slideInHorizontally(animationSpec = tween(220)) { w -> dir * w / 4 } + fadeIn()).togetherWith(
                    slideOutHorizontally(animationSpec = tween(220)) { w -> -dir * w / 4 } + fadeOut()
                )
            },
            label = "newTripStep",
        ) { current ->
            when (current) {
                0 -> StepName(
                    tripName      = tripName,
                    onTripName    = { tripName = it },
                    description   = description,
                    onDescription = { description = it },
                )
                1 -> StepDestination(
                    chosenPreset  = chosenPreset,
                    onPresetClick = { preset -> chosenPreset = preset; freeText = "" },
                    freeText      = freeText,
                    onFreeText    = { freeText = it; chosenPreset = null },
                )
                2 -> StepDates(
                    startDate    = startDate,
                    endDate      = endDate,
                    onStart      = { startDate = it; endDate = null },
                    onEnd        = { endDate   = it },
                    onClear      = { startDate = null; endDate = null },
                    onSkip       = { submit() },
                )
            }
        }

        // ── Footer nav ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                FilledIconButton(
                    onClick = goBack,
                    enabled = !isLoading,
                    shape   = AppShape.lg,
                    modifier = Modifier.size(52.dp),
                    colors  = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.generalBack)
                }
            }
            GradientButton(
                text      = when (step) {
                    0    -> s.newTripSetDestination
                    1    -> s.newTripPickDates
                    2    -> s.newTripCreate
                    else -> s.generalNext
                },
                onClick   = { if (step == 2) submit() else goNext() },
                enabled   = canAdvance && !isLoading,
                isLoading = isLoading && step == 2,
                modifier  = Modifier.weight(1f),
            )
        }
    }
}

// ── Step dots ───────────────────────────────────────────────────────────────

@Composable
private fun StepDots(step: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val active = i <= step
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else        MaterialTheme.colorScheme.surfaceVariant
                    ),
            )
        }
    }
}

// ── Step 1: Destination ─────────────────────────────────────────────────────

@Composable
private fun StepDestination(
    chosenPreset: PresetDestination?,
    onPresetClick: (PresetDestination) -> Unit,
    freeText: String,
    onFreeText: (String) -> Unit,
) {
    val s = LocalStrings.current
    val filtered = remember(freeText) {
        val q = freeText.trim().lowercase()
        if (q.isBlank()) presetDestinations
        else presetDestinations.filter { it.name.lowercase().contains(q) || it.country.lowercase().contains(q) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Column {
            Text(
                text       = s.newTripWhereGoing,
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text     = s.newTripDestSubtitle,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Free-text search / custom destination
        OutlinedTextField(
            value         = freeText,
            onValueChange = onFreeText,
            placeholder   = { Text(s.newTripDestSearch) },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine    = true,
            shape         = AppShape.lg,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction      = ImeAction.Done,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Preset grid (2 cols via manual chunked layout — keeps lazy+wrap simple)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            items(filtered.chunked(2)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    row.forEach { dest ->
                        DestinationTile(
                            preset    = dest,
                            isActive  = chosenPreset?.id == dest.id,
                            onClick   = { onPresetClick(dest) },
                            modifier  = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DestinationTile(
    preset: PresetDestination,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (isActive) MaterialTheme.colorScheme.primary
                 else          MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val bg     = if (isActive) MaterialTheme.colorScheme.primaryContainer
                 else          MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .clip(AppShape.lg)
            .background(bg)
            .border(width = if (isActive) 2.dp else 1.dp, color = border, shape = AppShape.lg)
            .clickable(onClick = onClick)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(AppShape.md)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            EmojiText(preset.emoji, fontSize = 22.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = preset.name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
            )
            Text(
                text     = preset.country,
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── Step 2: Dates (custom range calendar) ───────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
private fun StepDates(
    startDate: LocalDate?,
    endDate:   LocalDate?,
    onStart:   (LocalDate) -> Unit,
    onEnd:     (LocalDate) -> Unit,
    onClear:   () -> Unit,
    onSkip:    () -> Unit,
) {
    val s = LocalStrings.current
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    // We track the currently-visible month as a LocalDate anchored to day-1.
    // Arithmetic on LocalDate is straightforward; poking at `Month` enum directly
    // is fiddly across kotlinx-datetime versions.
    var visibleMonth by remember { mutableStateOf(LocalDate(today.year, today.month, 1)) }
    val viewYear  = visibleMonth.year
    val viewMonth = visibleMonth.month

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = s.newTripWhenGoing,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text     = s.newTripTapSetDates,
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSkip) {
                Text(s.newTripSkipDates, fontWeight = FontWeight.Bold)
            }
        }

        // Summary pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.lg)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Icon(
                imageVector        = Icons.Default.DateRange,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = formatRangeLabel(startDate, endDate, s),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (startDate != null && endDate != null) {
                    val nights = daysBetween(startDate, endDate)
                    Text(
                        text     = s.newTripNights(nights),
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (startDate != null) {
                TextButton(onClick = onClear) { Text(s.newTripClearDates) }
            }
        }

        // Calendar card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.xl)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            // Month nav
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { visibleMonth = visibleMonth.minus(1, DateTimeUnit.MONTH) },
                ) { Icon(Icons.Default.ChevronLeft, contentDescription = s.newTripPrevMonth) }

                Text(
                    text       = "${s.monthFullNames[viewMonth.ordinal]} $viewYear",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(
                    onClick = { visibleMonth = visibleMonth.plus(1, DateTimeUnit.MONTH) },
                ) { Icon(Icons.Default.ChevronRight, contentDescription = s.newTripNextMonth) }
            }

            // Day-of-week labels (Sun … Sat)
            Row(Modifier.fillMaxWidth()) {
                s.newTripWeekDayLabels.forEach { label ->
                    Text(
                        text      = label,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize  = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Day grid
            val firstOfMonth   = LocalDate(viewYear, viewMonth, 1)
            // Sunday-first offset (Sun=0 … Sat=6). kotlinx.DayOfWeek is 1..7 Mon..Sun.
            val firstDayOffset = (firstOfMonth.dayOfWeek.ordinal + 1) % 7
            val daysInMonth    = daysInMonth(viewYear, viewMonth)
            val totalCells     = firstDayOffset + daysInMonth
            val totalRows      = (totalCells + 6) / 7

            repeat(totalRows) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOffset + 1
                        if (dayNumber in 1..daysInMonth) {
                            val cellDate = LocalDate(viewYear, viewMonth, dayNumber)
                            CalendarCell(
                                date      = cellDate,
                                today     = today,
                                startDate = startDate,
                                endDate   = endDate,
                                onTap     = { date ->
                                    // Past dates are allowed — useful for logging past trips.
                                    when {
                                        startDate == null || endDate != null -> onStart(date)
                                        date == startDate                     -> onStart(date)
                                        date < startDate                      -> onStart(date)
                                        else                                  -> onEnd(date)
                                    }
                                },
                                modifier  = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(36.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    today: LocalDate,
    startDate: LocalDate?,
    endDate:   LocalDate?,
    onTap:     (LocalDate) -> Unit,
    modifier:  Modifier = Modifier,
) {
    val isStart = startDate == date
    val isEnd   = endDate   == date
    val inRange = startDate != null && endDate != null && date > startDate && date < endDate
    val isPast  = date < today
    val isToday = date == today

    val cellShape = AppShape.sm

    Box(
        modifier = modifier
            .height(36.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Range connector background
        if (inRange) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            )
        }
        // Start / end half-band (visual range continuation)
        if (isStart && endDate != null) {
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)))
            }
        }
        if (isEnd && startDate != null) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)))
                Spacer(Modifier.weight(1f))
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isStart || isEnd -> MaterialTheme.colorScheme.primary
                        else             -> Color.Transparent
                    }
                )
                .clickable { onTap(date) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "${date.day}",
                fontSize   = 12.sp,
                fontWeight = if (isStart || isEnd || isToday) FontWeight.Bold else FontWeight.Medium,
                color      = when {
                    isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
                    isPast           -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    inRange          -> MaterialTheme.colorScheme.primary
                    isToday          -> MaterialTheme.colorScheme.primary
                    else             -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// ── Step 0: Name ─────────────────────────────────────────────────────────────

@Composable
private fun StepName(
    tripName: String,
    onTripName: (String) -> Unit,
    description: String,
    onDescription: (String) -> Unit,
) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Column {
            Text(
                text       = s.newTripNameTitle,
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text     = s.newTripNameSubtitle,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value         = tripName,
            onValueChange = onTripName,
            label         = { Text(s.newTripNameLabel) },
            placeholder   = { Text(s.newTripNamePlaceholder) },
            leadingIcon   = { Icon(Icons.Default.Flight, contentDescription = null) },
            singleLine    = true,
            shape         = AppShape.lg,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction      = ImeAction.Next,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value         = description,
            onValueChange = onDescription,
            label         = { Text(s.newTripDescLabel) },
            placeholder   = { Text(s.newTripDescPlaceholder) },
            leadingIcon   = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            shape         = AppShape.lg,
            minLines      = 3,
            maxLines      = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction      = ImeAction.Done,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Date helpers ────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
private fun LocalDate.toEpochMillis(): Long =
    atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

private fun daysInMonth(year: Int, month: Month): Int {
    val start     = LocalDate(year, month, 1)
    val nextMonth = start.plus(1, DateTimeUnit.MONTH)
    // Days between day-1 of current month and day-1 of next month = days in this month.
    var d = 0
    var cursor = start
    while (cursor < nextMonth) { cursor = cursor.plus(1, DateTimeUnit.DAY); d++ }
    return d
}

private fun daysBetween(start: LocalDate, end: LocalDate): Int {
    var d = 0
    var cursor = start
    while (cursor < end) { cursor = cursor.plus(1, DateTimeUnit.DAY); d++ }
    return d
}

private fun formatRangeLabel(
    start:   LocalDate?,
    end:     LocalDate?,
    strings: com.domedemok.travelplanner.i18n.Strings,
): String {
    if (start == null) return strings.newTripSelectStart
    val sm = strings.monthShortNames[start.month.ordinal]
    if (end == null) return strings.newTripPickEnd(sm, start.day)
    val em = strings.monthShortNames[end.month.ordinal]
    return if (start.year == end.year) {
        if (start.month == end.month)
            "$sm ${start.day}–${end.day}, ${start.year}"
        else
            "$sm ${start.day} – $em ${end.day}, ${start.year}"
    } else {
        "$sm ${start.day}, ${start.year} – $em ${end.day}, ${end.year}"
    }
}
