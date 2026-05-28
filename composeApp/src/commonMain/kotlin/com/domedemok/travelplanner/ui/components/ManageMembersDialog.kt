package com.domedemok.travelplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.data.model.Trip
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMembersSheet(
    trip:           Trip,
    currentUserId:  String,
    onDismiss:      () -> Unit,
    onRemoveMember: (String) -> Unit,
    onRenameMember: (userId: String, newName: String) -> Unit,
) {
    val s = LocalStrings.current
    val isOwner        = currentUserId == trip.createdBy
    // Owner first, remaining members in arbitrary key order — same UI ordering as before.
    val activeMemberIds = listOf(trip.createdBy) + (trip.tripMembers.keys - trip.createdBy)

    // Rename dialog state
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameInput  by remember { mutableStateOf("") }

    // Remove confirmation dialog state
    var removeTarget by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Remove confirmation dialog — shown on top of the sheet
    removeTarget?.let { targetId ->
        val memberName = trip.tripMembers[targetId] ?: s.generalUnknown
        PopupThemeProvider {
            AlertDialog(
                onDismissRequest = { removeTarget = null },
                shape            = AppShape.xl,
                title            = { Text(s.membersRemoveTitle) },
                text             = { Text(s.membersRemoveMessage(memberName)) },
                confirmButton    = {
                    TextButton(
                        onClick = {
                            onRemoveMember(targetId)
                            removeTarget = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text(s.membersRemoveConfirm) }
                },
                dismissButton = {
                    TextButton(onClick = { removeTarget = null }) { Text(s.generalCancel) }
                },
            )
        }
    }

    // Rename confirmation dialog — shown on top of the sheet
    renameTarget?.let { targetId ->
        PopupThemeProvider {
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                shape            = AppShape.xl,
                title            = { Text(s.membersRenameTitle) },
                text             = {
                    OutlinedTextField(
                        value         = renameInput,
                        onValueChange = { renameInput = it },
                        label         = { Text(s.membersRenameLabel) },
                        singleLine    = true,
                        shape         = AppShape.lg,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick  = {
                            if (renameInput.isNotBlank()) {
                                onRenameMember(targetId, renameInput.trim())
                            }
                            renameTarget = null
                        },
                        enabled = renameInput.isNotBlank(),
                    ) { Text(s.membersRenameButton) }
                },
                dismissButton = {
                    TextButton(onClick = { renameTarget = null }) { Text(s.generalCancel) }
                },
            )
        }
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
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            // Header
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = s.membersTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 20.sp,
                )
                Text(
                    text  = s.membersPeopleCount(activeMemberIds.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(AppSpacing.xs))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                items(activeMemberIds) { memberId ->
                    val isMemberOwner = memberId == trip.createdBy
                    val memberName    = trip.tripMembers[memberId] ?: s.generalUnknown
                    val initials      = memberName.take(2).uppercase()

                    ElevatedCard(
                        shape     = AppShape.lg,
                        modifier  = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        colors    = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        ) {
                            // Avatar with initials
                            Box(
                                modifier         = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (isMemberOwner)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text       = initials,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (isMemberOwner)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }

                            // Name + role
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = memberName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 15.sp,
                                )
                                if (isMemberOwner) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Shield,
                                            contentDescription = null,
                                            modifier           = Modifier.size(11.dp),
                                            tint               = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text  = s.membersOwner,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            // Everyone can rename anyone — the rename is a trip-scoped alias
                            // and does not touch the Firebase Auth display name. Only the trip
                            // owner can remove other members (and not themselves).
                            IconButton(
                                onClick = {
                                    renameInput  = memberName
                                    renameTarget = memberId
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.DriveFileRenameOutline,
                                    contentDescription = s.generalRename,
                                    modifier           = Modifier.size(20.dp),
                                    tint               = MaterialTheme.colorScheme.primary,
                                )
                            }

                            if (isOwner && !isMemberOwner) {
                                IconButton(
                                    onClick  = { removeTarget = memberId },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.PersonRemove,
                                        contentDescription = s.generalRemove,
                                        modifier           = Modifier.size(20.dp),
                                        tint               = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.sm))

            // Close button
            OutlinedButton(
                onClick   = onDismiss,
                modifier  = Modifier.fillMaxWidth(),
                shape     = AppShape.lg,
            ) {
                Text(s.generalClose, fontWeight = FontWeight.SemiBold)
            }
        }
        } // PopupThemeProvider
    }
}
