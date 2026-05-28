package com.domedemok.travelplanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.util.JOIN_CODE_LENGTH
import com.domedemok.travelplanner.util.cleanJoinCode
import com.domedemok.travelplanner.viewmodel.JoinError
import com.domedemok.travelplanner.viewmodel.JoinTripViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Bottom-sheet version of the "join a trip" flow.
 * Consistent with all other modal sheets in the app (NewTripFlow, AddExpenseSheet, etc.).
 * Dismiss by swiping down or tapping the scrim — no explicit cancel button needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTripSheet(
    initialCode: String = "",
    onJoined: (tripId: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: JoinTripViewModel = koinViewModel(),
) {
    val s              = LocalStrings.current
    val state          by viewModel.uiState.collectAsState()
    var code           by remember { mutableStateOf(initialCode.uppercase()) }
    var showScanner    by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Navigate once join succeeds
    LaunchedEffect(state.joinedTripId) {
        state.joinedTripId?.let { onJoined(it) }
    }

    // Auto-focus the code field when the sheet appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val errorMessage = when (val err = state.error) {
        JoinError.InvalidCode   -> s.joinTripInvalidCode
        JoinError.AlreadyMember -> s.joinTripAlreadyMember
        is JoinError.Unknown    -> err.message
        null                    -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle       = { BottomSheetDefaults.DragHandle() },
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        PopupThemeProvider {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.xl)
                .padding(bottom = AppSpacing.lg)
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        ) {
            // ── Header icon ───────────────────────────────────────────────────
            Surface(
                shape    = AppShape.xl,
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector        = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier           = Modifier.size(32.dp),
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // ── Title + subtitle ──────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Text(
                    text       = s.joinTripTitle,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                )
                Text(
                    text      = s.joinTripSubtitle,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // ── Code input ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = code,
                onValueChange = { raw ->
                    code = cleanJoinCode(raw)
                    if (state.error != null) viewModel.clearError()
                },
                label         = { Text(s.joinTripCodeLabel) },
                placeholder   = { Text(s.joinTripCodePlaceholder) },
                singleLine    = true,
                isError       = errorMessage != null,
                supportingText = errorMessage?.let { msg ->
                    { Text(msg, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType   = KeyboardType.Ascii,
                    imeAction      = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { if (code.length == JOIN_CODE_LENGTH && !state.isLoading) viewModel.joinByCode(code) }
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 22.sp,
                    letterSpacing = 6.sp,
                    textAlign     = TextAlign.Center,
                ),
                shape   = AppShape.lg,
                colors  = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled  = !state.isLoading,
            )

            // ── Scan QR button ────────────────────────────────────────────────
            OutlinedButton(
                onClick  = { showScanner = true },
                enabled  = !state.isLoading,
                shape    = AppShape.lg,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(s.joinTripScanQr, fontWeight = FontWeight.SemiBold)
            }

            // ── Join button ───────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.joinByCode(code) },
                enabled  = code.length == JOIN_CODE_LENGTH && !state.isLoading,
                shape    = AppShape.lg,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                AnimatedVisibility(visible = state.isLoading, enter = fadeIn(), exit = fadeOut()) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                AnimatedVisibility(visible = !state.isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Text(s.joinTripJoin, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
        }
    }

    // ── QR Scanner overlay ────────────────────────────────────────────────────
    if (showScanner) {
        QrScannerDialog(
            onCodeScanned = { scannedCode ->
                code        = scannedCode
                showScanner = false
                if (scannedCode.length == JOIN_CODE_LENGTH) viewModel.joinByCode(scannedCode)
            },
            onDismiss = { showScanner = false },
        )
    }
}
