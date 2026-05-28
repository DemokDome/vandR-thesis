package com.domedemok.travelplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.*
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripDialog(
    trip: Trip,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, startDate: Long, endDate: Long) -> Unit,
    isLoading: Boolean = false,
) {
    val s = LocalStrings.current
    var name        by remember { mutableStateOf(trip.name) }
    var description by remember { mutableStateOf(trip.description) }
    var startDate   by remember { mutableStateOf(trip.startDate) }
    var endDate     by remember { mutableStateOf(trip.endDate) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = if (trip.startDate > 0) trip.startDate else null,
        initialSelectedEndDateMillis   = if (trip.endDate   > 0) trip.endDate   else null,
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDate = datePickerState.selectedStartDateMillis ?: 0L
                        endDate   = datePickerState.selectedEndDateMillis   ?: 0L
                        showDatePicker = false
                    },
                    // End date is optional — matches the create flow, where a
                    // trip can be saved with only a start date or no dates at all.
                    enabled = datePickerState.selectedStartDateMillis != null,
                ) { Text(s.generalOk) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(s.generalCancel) }
            },
        ) {
            DateRangePicker(
                state    = datePickerState,
                modifier = Modifier.weight(1f),
                title    = {
                    Text(s.editTripSelectDates, modifier = Modifier.padding(AppSpacing.lg))
                },
                headline = {
                    DateRangePickerDefaults.DateRangePickerHeadline(
                        selectedStartDateMillis = datePickerState.selectedStartDateMillis,
                        selectedEndDateMillis   = datePickerState.selectedEndDateMillis,
                        displayMode            = datePickerState.displayMode,
                        dateFormatter          = DatePickerDefaults.dateFormatter(),
                        modifier               = Modifier.padding(horizontal = AppSpacing.lg),
                    )
                },
            )
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
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = s.editTripTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 20.sp,
                )
            }

            Spacer(Modifier.height(AppSpacing.xs))

            // ── Name field ────────────────────────────────────────────────
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text(s.editTripNameLabel) },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next,
                ),
                shape   = AppShape.lg,
                colors  = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled  = !isLoading,
            )

            // ── Description field ─────────────────────────────────────────
            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                label         = { Text(s.editTripDescLabel) },
                maxLines      = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Done,
                ),
                shape   = AppShape.lg,
                colors  = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled  = !isLoading,
            )

            // ── Date picker trigger ───────────────────────────────────────
            OutlinedCard(
                onClick   = { showDatePicker = true },
                enabled   = !isLoading,
                shape     = AppShape.lg,
                modifier  = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.lg),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Icon(
                        imageVector        = Icons.Default.DateRange,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text  = s.editTripDatesLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text  = when {
                                startDate > 0L && endDate > 0L ->
                                    "${formatSimpleDate(startDate)}  –  ${formatSimpleDate(endDate)}"
                                startDate > 0L ->
                                    "${formatSimpleDate(startDate)}  ·  …"
                                else ->
                                    s.editTripTapSelect
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.xs))

            // ── Save button ───────────────────────────────────────────────
            Button(
                onClick  = { onConfirm(name, description, startDate, endDate) },
                enabled  = name.isNotBlank() && !isLoading,
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
                    Text(s.editTripSave, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick   = onDismiss,
                enabled   = !isLoading,
                shape     = AppShape.lg,
                modifier  = Modifier.fillMaxWidth(),
            ) {
                Text(s.generalCancel, fontWeight = FontWeight.SemiBold)
            }
        }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatSimpleDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val instant  = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.year}-${(dateTime.month.ordinal + 1).toString().padStart(2, '0')}-${dateTime.day.toString().padStart(2, '0')}"
}
