package com.domedemok.travelplanner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Expense
import com.domedemok.travelplanner.data.model.ExpenseCategory
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.screens.GradientButton
import com.domedemok.travelplanner.ui.components.EmojiText
import com.domedemok.travelplanner.ui.screens.budgetCategoryStyle
import com.domedemok.travelplanner.ui.screens.budgetFormatAmount
import com.domedemok.travelplanner.ui.theme.*
import kotlin.math.abs
import kotlin.math.round
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    tripMembers:   Map<String, String>,
    currentUserId: String?,
    expenseToEdit: Expense? = null,
    onDismiss:     () -> Unit,
    onConfirm:     (Expense) -> Unit,
    isLoading:     Boolean = false,
) {
    val s = LocalStrings.current
    val isEdit = expenseToEdit != null

    // Form state — seeded from expenseToEdit if editing
    var title      by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountText by remember {
        mutableStateOf(expenseToEdit?.amount?.let { budgetFormatAmount(it) } ?: "")
    }
    var category   by remember { mutableStateOf(expenseToEdit?.category ?: ExpenseCategory.OTHER) }
    var paidBy     by remember {
        mutableStateOf(
            expenseToEdit?.paidBy
                ?: currentUserId
                ?: tripMembers.keys.firstOrNull()
                ?: ""
        )
    }
    var splits     by remember {
        mutableStateOf(
            tripMembers.keys.associateWith { id ->
                expenseToEdit?.debts?.get(id)?.let { budgetFormatAmount(it) } ?: ""
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Live split accounting
    val total     = amountText.toDoubleOrNull() ?: 0.0
    val splitSum  = splits.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val remaining = round((total - splitSum) * 100) / 100.0
    val splitOk   = abs(remaining) < 0.01 && total > 0

    val canSave = title.isNotBlank() && total > 0 && paidBy.isNotBlank() && !isLoading

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
                .fillMaxHeight(0.93f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {

            // ── Sheet title ────────────────────────────────────────────────
            Text(
                text       = if (isEdit) s.expenseEditTitle else s.expenseNewTitle,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 20.sp,
            )

            // ── Description ────────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text(s.expenseDescLabel) },
                placeholder   = { Text(s.expenseDescPlaceholder) },
                singleLine    = true,
                shape         = AppShape.lg,
                modifier      = Modifier.fillMaxWidth(),
                enabled       = !isLoading,
                leadingIcon   = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            )

            // ── Amount ─────────────────────────────────────────────────────
            OutlinedTextField(
                value           = amountText,
                onValueChange   = { amountText = it },
                label           = { Text(s.expenseAmountLabel) },
                placeholder     = { Text("0.00") },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape           = AppShape.lg,
                modifier        = Modifier.fillMaxWidth(),
                enabled         = !isLoading,
                leadingIcon     = {
                    Text(
                        "€",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        modifier   = Modifier.padding(start = AppSpacing.sm),
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            )

            // ── Category chips ─────────────────────────────────────────────
            Text(
                text  = s.expenseCategoryLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val catScrollState = rememberScrollState()
            val catScope       = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .horizontalScroll(catScrollState)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            catScope.launch { catScrollState.scrollBy(-delta) }
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                ExpenseCategory.entries.forEach { cat ->
                    val meta     = budgetCategoryStyle(cat)
                    val selected = cat == category
                    Surface(
                        shape   = AppShape.pill,
                        color   = if (selected) meta.color else meta.bg,
                        border  = if (selected) null
                                  else BorderStroke(1.dp, meta.color.copy(alpha = 0.3f)),
                        onClick = { if (!isLoading) category = cat },
                    ) {
                        Row(
                            modifier              = Modifier.padding(
                                horizontal = AppSpacing.md,
                                vertical   = AppSpacing.sm,
                            ),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        ) {
                            EmojiText(meta.emoji, fontSize = 13.sp)
                            Text(
                                // Localised label; falls back to the enum's English displayName if a translation is missing.
                                text       = s.expenseCategoryLabels[cat] ?: cat.displayName,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (selected) Color.White else meta.color,
                            )
                        }
                    }
                }
            }

            // ── Paid by chips ──────────────────────────────────────────────
            Text(
                text  = s.expensePaidByLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val paidByScrollState = rememberScrollState()
            val paidByScope       = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .horizontalScroll(paidByScrollState)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            paidByScope.launch { paidByScrollState.scrollBy(-delta) }
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                tripMembers.keys.forEach { memberId ->
                    val isSelected = memberId == paidBy
                    val name = tripMembers[memberId] ?: ""
                    FilterChip(
                        selected = isSelected,
                        onClick  = { if (!isLoading) paidBy = memberId },
                        label    = {
                            Text(
                                text       = name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            // ── Split section ──────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = s.expenseSplitLabel,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                // Live remaining indicator
                val indicatorColor = when {
                    total == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    splitOk      -> Green500
                    else         -> MaterialTheme.colorScheme.error
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (total > 0) {
                        Icon(
                            imageVector        = if (splitOk) Icons.Default.CheckCircle
                                                 else Icons.Default.Error,
                            contentDescription = null,
                            modifier           = Modifier.size(14.dp),
                            tint               = indicatorColor,
                        )
                    }
                    Text(
                        text = when {
                            total == 0.0  -> s.expenseSplitEnterAmount
                            splitOk       -> s.expenseSplitBalanced
                            remaining > 0 -> s.expenseSplitLeft(budgetFormatAmount(remaining))
                            else          -> s.expenseSplitOver(budgetFormatAmount(-remaining))
                        },
                        fontSize   = 11.sp,
                        color      = indicatorColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Split-equally shortcut
            FilledTonalButton(
                onClick  = {
                    if (total > 0 && tripMembers.isNotEmpty()) {
                        // Distribute evenly, last person absorbs rounding remainder
                        val base   = kotlin.math.floor(total / tripMembers.size * 100).toLong()
                        val extra  = round(total * 100).toLong() - base * tripMembers.size
                        val ids    = tripMembers.keys.toList()
                        val newSplits = ids.mapIndexed { idx, id ->
                            val cents = if (idx == ids.lastIndex) base + extra else base
                            id to budgetFormatAmount(cents / 100.0)
                        }.toMap()
                        splits = newSplits
                    }
                },
                enabled  = !isLoading && total > 0,
                shape    = AppShape.lg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(AppSpacing.sm))
                Text(s.expenseSplitEqually, fontWeight = FontWeight.SemiBold)
            }

            // Per-member amount fields
            tripMembers.forEach { (id, name) ->
                val meta = budgetCategoryStyle(category)
                OutlinedTextField(
                    value           = splits[id] ?: "",
                    onValueChange   = { v -> splits = splits.toMutableMap().apply { put(id, v) } },
                    label           = { Text(name) },
                    placeholder     = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    shape           = AppShape.lg,
                    modifier        = Modifier.fillMaxWidth(),
                    enabled         = !isLoading,
                    leadingIcon     = {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(meta.bg),
                            contentAlignment = Alignment.Center,
                        ) { EmojiText(meta.emoji, fontSize = 12.sp) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                )
            }

            Spacer(Modifier.height(AppSpacing.sm))

            // ── Save button ────────────────────────────────────────────────
            GradientButton(
                text      = if (isEdit) s.expenseSaveChanges else s.expenseAddButton,
                onClick   = {
                    val parsedDebts = splits.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                    // If no splits filled in, assign everything to the payer
                    val finalDebts  = if (parsedDebts.values.sum() < 0.01) {
                        mapOf(paidBy to total)
                    } else {
                        parsedDebts
                    }
                    onConfirm(
                        Expense(
                            id       = expenseToEdit?.id ?: "",
                            title    = title.trim(),
                            amount   = total,
                            category = category,
                            paidBy   = paidBy,
                            debts    = finalDebts,
                            date     = expenseToEdit?.date ?: 0L,
                        )
                    )
                },
                enabled   = canSave,
                isLoading = isLoading,
            )
        }
        }
    }
}
