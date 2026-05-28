package com.domedemok.travelplanner.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.domedemok.travelplanner.data.model.TripPhoto
import com.domedemok.travelplanner.viewmodel.GalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun GalleryScreen(
    tripId:        String,
    currentUserId: String?,
    isOwner:       Boolean,
    modifier:      Modifier,
) {
    val viewModel: GalleryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Start streaming photos once
    LaunchedEffect(tripId) { viewModel.loadPhotos(tripId) }

    // Modern photo picker (no READ_EXTERNAL_STORAGE permission needed on API 33+)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Activity-result callbacks run on the main thread — dispatch IO work off it
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes != null) viewModel.uploadPhoto(tripId, bytes)
        }
    }

    GalleryGrid(
        photos           = uiState.photos,
        isLoading        = uiState.isLoading,
        isUploading      = uiState.isUploading,
        error            = uiState.error,
        currentUserId    = currentUserId,
        isOwner          = isOwner,
        onDeletePhoto    = { viewModel.deletePhoto(tripId, it) },
        onDownloadPhoto  = { photo -> downloadPhoto(context, photo) },
        onAddPhotoClick  = {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onDismissError   = { viewModel.clearError() },
        modifier         = modifier,
    )
}

private fun downloadPhoto(context: Context, photo: TripPhoto) {
    val filename = "photo_${photo.id}.jpg"
    val request  = DownloadManager.Request(Uri.parse(photo.storageUrl))
        .setTitle(filename)
        .setDescription("Downloading photo…")
        .setMimeType("image/jpeg")
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, filename)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
}
