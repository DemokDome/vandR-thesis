package com.domedemok.travelplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.domedemok.travelplanner.data.model.TripPhoto
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.theme.AppShape
import com.domedemok.travelplanner.ui.theme.AppSpacing
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Expect declaration ────────────────────────────────────────────────────────
// Platform actuals (GalleryScreen.android.kt / GalleryScreen.js.kt) handle
// image picking, then delegate display to the shared GalleryGrid below.

@Composable
expect fun GalleryScreen(
    tripId:       String,
    currentUserId: String?,
    isOwner:      Boolean,
    modifier:     Modifier = Modifier,
)

// ── Shared display composable ─────────────────────────────────────────────────
// Called by both platform actuals. Owns selection, delete-confirm and
// fullscreen-viewer state; delegates upload FAB click to [onAddPhotoClick].

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GalleryGrid(
    photos:           List<TripPhoto>,
    isLoading:        Boolean,
    isUploading:      Boolean,
    error:            String?,
    currentUserId:    String?,
    isOwner:          Boolean,
    onDeletePhoto:    (TripPhoto) -> Unit,
    onDownloadPhoto:  (TripPhoto) -> Unit,
    onAddPhotoClick:  () -> Unit,
    onDismissError:   () -> Unit,
    modifier:         Modifier = Modifier,
) {
    val s = LocalStrings.current

    var selectedPhoto by remember { mutableStateOf<TripPhoto?>(null) }
    var photoToDelete by remember { mutableStateOf<TripPhoto?>(null) }
    var isEditMode    by remember { mutableStateOf(false) }

    // Can this user delete at least one photo?
    val canDeleteAny = isOwner || photos.any { it.uploadedBy == currentUserId }

    // Exit edit mode automatically if there are no more photos
    if (photos.isEmpty()) isEditMode = false

    Box(modifier = modifier) {

        when {
            // ── Loading spinner (first load only) ────────────────────────────
            isLoading && photos.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // ── Empty state ───────────────────────────────────────────────────
            photos.isEmpty() -> {
                Column(
                    modifier              = Modifier
                        .align(Alignment.Center)
                        .padding(AppSpacing.xl),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Text(text = "📷", style = MaterialTheme.typography.displayLarge)
                    Text(
                        text       = s.galleryEmpty,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text      = s.galleryEmptyBody,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── Photo grid ────────────────────────────────────────────────────
            else -> {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(3),
                    modifier              = Modifier.fillMaxSize(),
                    contentPadding        = PaddingValues(start = 2.dp, end = 2.dp, bottom = 2.dp),
                    verticalArrangement   = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(photos, key = { it.id }) { photo ->
                        val canDelete = isOwner || photo.uploadedBy == currentUserId
                        PhotoThumbnail(
                            photo         = photo,
                            showDeleteBtn = isEditMode && canDelete,
                            onClick       = { if (!isEditMode) selectedPhoto = photo },
                            onDeleteClick = { photoToDelete = photo },
                        )
                    }
                    // Bottom padding so FAB doesn't cover last row
                    item(span = { GridItemSpan(3) }) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // ── Delete / Done FAB (bottom-left, only when photos exist and user can delete) ──
        if (photos.isNotEmpty() && canDeleteAny) {
            FloatingActionButton(
                onClick        = { isEditMode = !isEditMode },
                modifier       = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSpacing.lg)
                    .navigationBarsPadding(),
                containerColor = if (isEditMode)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.errorContainer,
                contentColor   = if (isEditMode)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Icon(
                    imageVector        = if (isEditMode) Icons.Default.Check else Icons.Default.Delete,
                    contentDescription = if (isEditMode) s.generalSave else s.generalDelete,
                )
            }
        }

        // ── Upload progress bar ───────────────────────────────────────────────
        AnimatedVisibility(
            visible  = isUploading,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            enter    = fadeIn(),
            exit     = fadeOut(),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // ── FAB (Add Photo) — hidden in edit mode ─────────────────────────────
        AnimatedVisibility(
            visible  = !isEditMode,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.lg)
                .navigationBarsPadding(),
            enter    = fadeIn(),
            exit     = fadeOut(),
        ) {
            FloatingActionButton(
                onClick        = { if (!isUploading) onAddPhotoClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = s.galleryAddPhoto)
                }
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        if (error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = AppSpacing.md, end = AppSpacing.md),
                action = {
                    TextButton(onClick = onDismissError) { Text(s.generalDismiss) }
                },
            ) { Text(error) }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            shape            = AppShape.xl,
            title            = { Text(s.galleryDeleteTitle) },
            text             = { Text(s.galleryDeleteBody) },
            confirmButton    = {
                TextButton(
                    onClick = { onDeletePhoto(photo); photoToDelete = null },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(s.generalDelete) }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) { Text(s.generalCancel) }
            },
        )
    }

    // ── Fullscreen viewer (only available outside edit mode) ──────────────────
    selectedPhoto?.let { photo ->
        PhotoViewer(
            photo      = photo,
            onDismiss  = { selectedPhoto = null },
            onDownload = { onDownloadPhoto(photo) },
        )
    }
}

// ── Single thumbnail cell ─────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumbnail(
    photo:         TripPhoto,
    showDeleteBtn: Boolean,
    onClick:       () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .combinedClickable(onClick = onClick),
    ) {
        SubcomposeAsyncImage(
            model              = photo.storageUrl,
            contentDescription = photo.caption.ifEmpty { null },
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp).align(Alignment.Center),
                        strokeWidth = 2.dp,
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )

        // Delete badge — only visible when edit mode is active for this photo
        AnimatedVisibility(
            visible  = showDeleteBtn,
            modifier = Modifier.align(Alignment.TopStart),
            enter    = fadeIn(),
            exit     = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(50))
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Remove,
                    contentDescription = "Delete photo",
                    tint               = Color.White,
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Fullscreen viewer dialog ──────────────────────────────────────────────────

@Composable
private fun PhotoViewer(
    photo:      TripPhoto,
    onDismiss:  () -> Unit,
    onDownload: () -> Unit,
) {
    val s = LocalStrings.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            // Photo
            AsyncImage(
                model              = photo.storageUrl,
                contentDescription = photo.caption.ifEmpty { null },
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            )

            // Top bar: close (left) + download (right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(AppSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = s.generalClose,
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                    )
                }
            }

            // Caption + uploader info (bottom)
            val infoText = buildString {
                if (photo.uploaderName.isNotEmpty()) {
                    append(s.galleryBy)
                    append(" ")
                    append(photo.uploaderName)
                }
                if (photo.caption.isNotEmpty()) {
                    if (isNotEmpty()) append("  ·  ")
                    append(photo.caption)
                }
                if (photo.uploadedAt > 0L) {
                    if (isNotEmpty()) append("  ·  ")
                    append(formatPhotoDate(photo.uploadedAt))
                }
            }
            if (infoText.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color    = Color.Black.copy(alpha = 0.55f),
                ) {
                    Text(
                        text      = infoText,
                        color     = Color.White,
                        fontSize  = 13.sp,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis,
                        modifier  = Modifier
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
    }
}

// ── Utility ───────────────────────────────────────────────────────────────────

private fun formatPhotoDate(epochMs: Long): String {
    return try {
        val dt = Instant.fromEpochMilliseconds(epochMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val m  = dt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        "$m ${dt.day}, ${dt.year}"
    } catch (_: Exception) { "" }
}
