package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Expense
import com.domedemok.travelplanner.data.model.ExpenseCategory
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.i18n.localizedLabel
import com.domedemok.travelplanner.ui.components.AddExpenseSheet
import com.domedemok.travelplanner.ui.components.EmojiText
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.BudgetViewModel
import com.domedemok.travelplanner.viewmodel.DebtOperation
import com.domedemok.travelplanner.viewmodel.UserBudgetSummary
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ── Category visual metadata ─────────────────────────────────────────────────

internal data class BudgetCategoryStyle(val emoji: String, val color: Color, val bg: Color)

internal val budgetCategoryStyles = mapOf(
    ExpenseCategory.FOOD_AND_DRINK          to BudgetCategoryStyle("🍽️", Color(0xFFEA580C), Color(0xFFFED7AA)),
    ExpenseCategory.TRANSPORTATION          to BudgetCategoryStyle("🚆", Color(0xFF6D5FE8), Color(0xFFECEAFD)),
    ExpenseCategory.ACCOMMODATION           to BudgetCategoryStyle("🏨", Color(0xFF0D9488), Color(0xFFCCFBF1)),
    ExpenseCategory.ACTIVITIES_AND_TICKETS  to BudgetCategoryStyle("🎡", Color(0xFF7C3AED), Color(0xFFEDE9FE)),
    ExpenseCategory.SHOPPING                to BudgetCategoryStyle("🛍️", Color(0xFFDB2777), Color(0xFFFCE7F3)),
    ExpenseCategory.OTHER                   to BudgetCategoryStyle("📦", Color(0xFF475569), Color(0xFFF1F5F9)),
)

internal fun budgetCategoryStyle(cat: ExpenseCategory): BudgetCategoryStyle =
    budgetCategoryStyles[cat] ?: budgetCategoryStyles[ExpenseCategory.OTHER]!!

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun BudgetScreen(
    tripId:        String,
    /** Active members — used for picker / split iteration in AddExpenseSheet and MemberBreakdownCard. */
    tripMembers:   Map<String, String>,
    /** Ex-members whose names must still resolve on historical splits but who can no longer be assigned new expenses. */
    formerMembers: Map<String, String> = emptyMap(),
    modifier:      Modifier = Modifier,
    viewModel:     BudgetViewModel = koinViewModel(),
) {
    val s = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()

    // Combined map for name resolution on historical expense entries.
    // Active wins on key collisions (the rare reuse-of-uid case after rejoin).
    val nameLookup = remember(tripMembers, formerMembers) { formerMembers + tripMembers }

    var showAddSheet     by remember { mutableStateOf(false) }
    var editingExpense   by remember { mutableStateOf<Expense?>(null) }
    var detailExpense    by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(tripId) {
        viewModel.loadExpenses(tripId, tripMembers.keys.toList())
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize(),
                contentPadding  = PaddingValues(
                    start  = AppSpacing.lg,
                    end    = AppSpacing.lg,
                    top    = AppSpacing.lg,
                    bottom = AppSpacing.xxxl + AppSpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                // ── Hero total card ───────────────────────────────────────────
                item {
                    BudgetHeroCard(
                        total              = uiState.totalSpent,
                        expenseCount       = uiState.expenses.size,
                        expensesByCategory = uiState.expensesByCategory,
                    )
                }

                // ── Settlement (who owes whom) ─────────────────────────────────
                if (uiState.debts.isNotEmpty()) {
                    item { BudgetSectionLabel(s.budgetSettlement) }
                    item {
                        SettlementCard(
                            debts       = uiState.debts,
                            tripMembers = nameLookup,   // historical names included
                        )
                    }
                }

                // ── Per-person summary (only relevant for groups) ─────────────
                if (tripMembers.size > 1) {
                    item { BudgetSectionLabel(s.budgetPerPerson) }
                    item {
                        MemberBreakdownCard(
                            tripMembers    = tripMembers,
                            userSummaries  = uiState.userSummaries,
                            selectedUserId = uiState.selectedUserId,
                            currentUserId  = uiState.currentUserId,
                            onSelectUser   = { viewModel.selectUser(it) },
                        )
                    }
                }

                // ── Expense list ──────────────────────────────────────────────
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(top = AppSpacing.xs),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BudgetSectionLabel(s.budgetExpenses)
                        if (uiState.expenses.isNotEmpty()) {
                            Surface(
                                shape = AppShape.pill,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text       = "${uiState.expenses.size}",
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }

                if (uiState.expenses.isEmpty()) {
                    item { EmptyBudgetState() }
                } else {
                    items(
                        items = uiState.expenses.sortedByDescending { it.date },
                        key   = { it.id },
                    ) { expense ->
                        val payerName = nameLookup[expense.paidBy] ?: s.generalUnknown
                        ExpenseCard(
                            expense   = expense,
                            payerName = payerName,
                            onClick   = { detailExpense = expense },
                        )
                    }
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        ExtendedFloatingActionButton(
            onClick        = { showAddSheet = true },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.xl),
            shape          = AppShape.pill,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            icon           = { Icon(Icons.Default.Add, contentDescription = null) },
            text           = { Text(s.budgetAddExpense, fontWeight = FontWeight.Bold) },
        )

        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(AppSpacing.lg),
                action   = { TextButton(onClick = { viewModel.clearError() }) { Text(s.generalDismiss) } },
            ) { Text(error) }
        }
    }

    // ── Add / Edit sheet ──────────────────────────────────────────────────────
    if (showAddSheet) {
        AddExpenseSheet(
            tripMembers   = tripMembers,
            currentUserId = uiState.currentUserId,
            onDismiss     = { showAddSheet = false },
            onConfirm     = { expense ->
                viewModel.addExpense(tripId, expense)
                showAddSheet = false
            },
            isLoading     = uiState.isSaving,
        )
    }

    if (editingExpense != null) {
        AddExpenseSheet(
            tripMembers   = tripMembers,
            currentUserId = uiState.currentUserId,
            expenseToEdit = editingExpense,
            onDismiss     = { editingExpense = null },
            onConfirm     = { updated ->
                viewModel.updateExpense(tripId, updated)
                editingExpense = null
            },
            isLoading     = uiState.isSaving,
        )
    }

    // ── Detail sheet ──────────────────────────────────────────────────────────
    val detail = detailExpense
    if (detail != null) {
        ExpenseDetailSheet(
            expense       = detail,
            tripMembers   = nameLookup,   // historical names included

            currentUserId = uiState.currentUserId,
            onDismiss     = { detailExpense = null },
            onEdit        = { detailExpense = null; editingExpense = detail },
            onDelete      = {
                viewModel.deleteExpense(tripId, detail.id)
                detailExpense = null
            },
        )
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun BudgetHeroCard(
    total:              Double,
    expenseCount:       Int,
    expensesByCategory: Map<ExpenseCategory, Double>,
) {
    val s = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppGradients.primaryButton, AppShape.xl)
            .padding(AppSpacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(
                text     = s.budgetTotalSpent,
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.75f),
            )
            Text(
                text          = budgetFormatAmount(total),
                fontSize      = 40.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = Color.White,
                letterSpacing = (-1).sp,
            )
            Text(
                text  = if (expenseCount == 0) s.budgetNoExpenses else s.budgetExpenseCount(expenseCount),
                fontSize = 12.sp,
                color    = Color.White.copy(alpha = 0.6f),
            )

            // Top-4 categories as pills
            if (expensesByCategory.isNotEmpty()) {
                Spacer(Modifier.height(AppSpacing.xs))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    items(
                        expensesByCategory.entries
                            .sortedByDescending { it.value }
                            .take(4)
                    ) { (cat, amount) ->
                        val style = budgetCategoryStyle(cat)
                        Surface(
                            shape = AppShape.pill,
                            color = Color.White.copy(alpha = 0.18f),
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = AppSpacing.sm + 2.dp, vertical = 4.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            ) {
                                EmojiText(style.emoji, fontSize = 12.sp)
                                Text(
                                    text       = budgetFormatAmount(amount),
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Settlement card ───────────────────────────────────────────────────────────

@Composable
private fun SettlementCard(
    debts:       List<DebtOperation>,
    tripMembers: Map<String, String>,
) {
    val s = LocalStrings.current
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier            = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            debts.forEachIndexed { i, debt ->
                val debtorName   = tripMembers[debt.debtorId]   ?: s.generalUnknown
                val creditorName = tripMembers[debt.creditorId] ?: s.generalUnknown

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    // Debtor
                    Surface(
                        shape = AppShape.sm,
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text       = debtorName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp,
                            color      = MaterialTheme.colorScheme.onErrorContainer,
                            modifier   = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp),
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Creditor
                    Surface(
                        shape = AppShape.sm,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text       = creditorName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier   = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp),
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    // Amount
                    Surface(
                        shape = AppShape.pill,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    ) {
                        Text(
                            text       = budgetFormatAmount(debt.amount),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 13.sp,
                            color      = MaterialTheme.colorScheme.error,
                            modifier   = Modifier.padding(horizontal = AppSpacing.md, vertical = 4.dp),
                        )
                    }
                }

                if (i < debts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    )
                }
            }
        }
    }
}

// ── Member breakdown card ─────────────────────────────────────────────────────

@Composable
private fun MemberBreakdownCard(
    tripMembers:    Map<String, String>,
    userSummaries:  Map<String, UserBudgetSummary>,
    selectedUserId: String?,
    currentUserId:  String?,
    onSelectUser:   (String) -> Unit,
) {
    val s = LocalStrings.current
    val summary      = userSummaries[selectedUserId] ?: UserBudgetSummary()
    val selectedName = tripMembers[selectedUserId] ?: s.generalUnknown

    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier            = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            // Member picker
            val memberScrollState = rememberScrollState()
            val memberScope       = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .horizontalScroll(memberScrollState)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            memberScope.launch { memberScrollState.scrollBy(-delta) }
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                tripMembers.keys.forEach { memberId ->
                    val isSelected = memberId == selectedUserId
                    val name = tripMembers[memberId] ?: ""
                    FilterChip(
                        selected = isSelected,
                        onClick  = { onSelectUser(memberId) },
                        label    = {
                            Text(
                                text       = name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Stats
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                BudgetStatBlock(
                    label = s.budgetPaid,
                    value = budgetFormatAmount(summary.totalPaid),
                    color = MaterialTheme.colorScheme.primary,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                BudgetStatBlock(
                    label = s.budgetShare,
                    value = budgetFormatAmount(summary.totalShare),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                val balanceColor = when {
                    summary.netBalance >  0.01 -> Green500
                    summary.netBalance < -0.01 -> MaterialTheme.colorScheme.error
                    else                       -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val prefix = if (summary.netBalance > 0.01) "+" else ""
                BudgetStatBlock(
                    label = s.budgetBalance,
                    value = "$prefix${budgetFormatAmount(summary.netBalance)}",
                    color = balanceColor,
                    bold  = true,
                )
            }

            // Plain-English balance line
            val balanceText = when {
                summary.netBalance >  0.01 -> s.budgetOwedBack(selectedName, budgetFormatAmount(summary.netBalance))
                summary.netBalance < -0.01 -> s.budgetOwes(selectedName, budgetFormatAmount(-summary.netBalance))
                else                       -> s.budgetSettledUp(selectedName)
            }
            Text(
                text     = balanceText,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BudgetStatBlock(
    label: String,
    value: String,
    color: Color,
    bold:  Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text       = value,
            fontSize   = 17.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color      = color,
        )
    }
}

// ── Individual expense card ────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
private fun ExpenseCard(
    expense:   Expense,
    payerName: String,
    onClick:   () -> Unit,
) {
    val s = LocalStrings.current
    val style = budgetCategoryStyle(expense.category)

    ElevatedCard(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = AppShape.xl,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppElevation.card),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier              = Modifier.padding(AppSpacing.md),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            // Category icon box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(AppShape.md)
                    .background(style.bg),
                contentAlignment = Alignment.Center,
            ) {
                EmojiText(style.emoji, fontSize = 22.sp)
            }

            // Content
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text       = expense.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    Surface(shape = AppShape.sm, color = style.bg) {
                        Text(
                            text       = s.expenseCategoryLabels[expense.category] ?: expense.category.displayName,
                            fontSize   = 10.sp,
                            color      = style.color,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text     = "· $payerName ${s.budgetPaidSuffix}",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (expense.date > 0L) {
                        Text(
                            text     = "· ${budgetFormatShortDate(expense.date, s)}",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Amount
            Text(
                text       = budgetFormatAmount(expense.amount),
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 15.sp,
                color      = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ── Expense detail bottom sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun ExpenseDetailSheet(
    expense:       Expense,
    tripMembers:   Map<String, String>,
    currentUserId: String?,
    onDismiss:     () -> Unit,
    onEdit:        () -> Unit,
    onDelete:      () -> Unit,
) {
    val s          = LocalStrings.current
    var confirmDelete by remember { mutableStateOf(false) }
    val style      = budgetCategoryStyle(expense.category)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val payerName  = tripMembers[expense.paidBy] ?: s.generalUnknown

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = AppShape.xl,
            title = { Text(s.budgetDeleteExpenseTitle) },
            text  = { Text(s.budgetDeleteExpenseBody(expense.title)) },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(s.generalDelete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.generalCancel) } }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            // Header row: icon + title + edit/delete
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(AppShape.md)
                        .background(style.bg),
                    contentAlignment = Alignment.Center,
                ) { EmojiText(style.emoji, fontSize = 26.sp) }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = expense.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Text(
                        text       = expense.category.localizedLabel(s),
                        fontSize   = 12.sp,
                        color      = style.color,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, s.generalEdit, tint = Amber600)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.DeleteOutline, s.generalDelete, tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Amount
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(s.budgetExpenseTotal, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(
                    text       = budgetFormatAmount(expense.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 24.sp,
                )
            }

            // Paid by
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(s.expensePaidByLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Surface(shape = AppShape.pill, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text       = payerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier   = Modifier.padding(horizontal = AppSpacing.md, vertical = 4.dp),
                    )
                }
            }

            // Date
            if (expense.date > 0L) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(s.budgetExpenseDate, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(budgetFormatShortDate(expense.date, s), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Split breakdown
            val splitEntries = expense.debts.filter { it.value > 0.005 }
            if (splitEntries.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Text(
                    text       = s.expenseSplitLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                splitEntries.forEach { (userId, share) ->
                    val name = tripMembers[userId] ?: s.generalUnknown
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(name, fontSize = 13.sp)
                        Text(budgetFormatAmount(share), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyBudgetState() {
    val s = LocalStrings.current
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Surface(
            shape    = CircleShape,
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Text("💸", fontSize = 32.sp) }
        }
        Spacer(Modifier.height(AppSpacing.sm))
        Text(
            text       = s.budgetNoExpenses,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text      = s.budgetNoExpensesBody,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun BudgetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Black,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp,
        modifier      = modifier.padding(top = AppSpacing.xs),
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

// Always shows exactly 2 decimal places. Safe for negative amounts.
internal fun budgetFormatAmount(amount: Double): String {
    val sign  = if (amount < -0.005) "-" else ""
    val abs   = kotlin.math.abs(amount)
    val cents = kotlin.math.round(abs * 100).toLong()
    val units = cents / 100
    val rem   = cents % 100
    return "$sign$units.${rem.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalTime::class)
private fun budgetFormatShortDate(epochMs: Long, strings: com.domedemok.travelplanner.i18n.Strings): String {
    if (epochMs == 0L) return ""
    val validMs = if (epochMs < 1_000_000_000_000L) epochMs * 1000L else epochMs
    val dt      = Instant.fromEpochMilliseconds(validMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${strings.monthShortNames[dt.month.ordinal]} ${dt.day}"
}
