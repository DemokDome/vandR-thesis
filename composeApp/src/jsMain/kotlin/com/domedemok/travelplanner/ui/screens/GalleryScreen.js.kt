package com.domedemok.travelplanner.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.domedemok.travelplanner.data.model.TripPhoto
import com.domedemok.travelplanner.viewmodel.GalleryViewModel
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.koin.compose.viewmodel.koinViewModel
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.xhr.BLOB
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType

@Composable
actual fun GalleryScreen(
    tripId:        String,
    currentUserId: String?,
    isOwner:       Boolean,
    modifier:      Modifier,
) {
    val viewModel: GalleryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope   = rememberCoroutineScope()

    // Start streaming photos once
    LaunchedEffect(tripId) { viewModel.loadPhotos(tripId) }

    // Image picker using a hidden <input type="file"> element
    val pickImage: () -> Unit = {
        val input = document.createElement("input") as HTMLInputElement
        input.type   = "file"
        input.accept = "image/*"
        input.style.display = "none"

        input.onchange = { _ ->
            val file = input.files?.item(0)

            // Always clean up the DOM element first
            runCatching { document.body?.removeChild(input) }

            if (file != null) {
                val reader = FileReader()
                reader.onerror = { _ ->
                    // FileReader failed — surface an error so the user knows
                    viewModel.setError("Could not read the selected image.")
                }
                reader.onload = { _ ->
                    val buffer = reader.result as? ArrayBuffer
                    if (buffer != null) {
                        val bytes = Int8Array(buffer).unsafeCast<ByteArray>()
                        scope.launch { viewModel.uploadPhoto(tripId, bytes) }
                    }
                    // null result treated as silent no-op (should never happen after onload)
                }
                reader.readAsArrayBuffer(file)
            }
        }

        document.body?.appendChild(input)
        input.click()
    }

    GalleryGrid(
        photos           = uiState.photos,
        isLoading        = uiState.isLoading,
        isUploading      = uiState.isUploading,
        error            = uiState.error,
        currentUserId    = currentUserId,
        isOwner          = isOwner,
        onDeletePhoto    = { viewModel.deletePhoto(tripId, it) },
        onDownloadPhoto  = { photo -> downloadPhoto(photo) },
        onAddPhotoClick  = pickImage,
        onDismissError   = { viewModel.clearError() },
        modifier         = modifier,
    )
}

private fun downloadPhoto(photo: TripPhoto) {
    val filename = "photo_${photo.id}.jpg"
    val xhr = XMLHttpRequest()
    xhr.open("GET", photo.storageUrl, true)
    xhr.responseType = XMLHttpRequestResponseType.BLOB
    xhr.onload = {
        if (xhr.status.toInt() == 200) {
            val blob    = xhr.response as Blob
            val blobUrl = URL.createObjectURL(blob)
            val a       = document.createElement("a") as HTMLAnchorElement
            a.href      = blobUrl
            a.download  = filename
            document.body?.appendChild(a)
            a.click()
            document.body?.removeChild(a)
            URL.revokeObjectURL(blobUrl)
        }
    }
    xhr.send()
}
