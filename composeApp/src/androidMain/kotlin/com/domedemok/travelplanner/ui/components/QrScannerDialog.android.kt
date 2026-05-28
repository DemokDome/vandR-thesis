package com.domedemok.travelplanner.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.util.JOIN_CODE_LENGTH
import com.domedemok.travelplanner.util.cleanJoinCode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
actual fun QrScannerDialog(onCodeScanned: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionDenied  by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) permissionGranted = true else permissionDenied = true
    }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth  = false,
            decorFitsSystemWindows   = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            when {
                permissionGranted -> CameraScanner(onCodeScanned = onCodeScanned)
                permissionDenied  -> PermissionDeniedContent(message = s.joinTripCameraPermission)
                // else: waiting for system permission dialog — show nothing extra
            }

            // Close button always on top
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = s.generalClose, tint = Color.White)
            }
        }
    }
}

// ── Camera preview + ML Kit barcode analysis ──────────────────────────────────

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScanner(onCodeScanned: (String) -> Unit) {
    val s              = LocalStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // AtomicBoolean prevents multiple callbacks if two frames are processed concurrently
    val scanned        = remember { AtomicBoolean(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Camera preview ────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor    = Executors.newSingleThreadExecutor()

                ProcessCameraProvider.getInstance(ctx).also { future ->
                    future.addListener({
                        val provider = future.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val scannerOptions = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        val barcodeScanner = BarcodeScanning.getClient(scannerOptions)

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(executor) { proxy ->
                            if (scanned.get()) { proxy.close(); return@setAnalyzer }

                            val mediaImage = proxy.image
                            if (mediaImage == null) { proxy.close(); return@setAnalyzer }

                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                proxy.imageInfo.rotationDegrees,
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { codes ->
                                    codes.firstOrNull()?.rawValue?.let { raw ->
                                        val code = cleanJoinCode(raw)
                                        if (code.length == JOIN_CODE_LENGTH && scanned.compareAndSet(false, true)) {
                                            // Post to main thread — onCodeScanned is a Compose callback
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onCodeScanned(code)
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener { proxy.close() }
                        }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }

                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Viewfinder overlay ────────────────────────────────────────────────
        Box(
            modifier          = Modifier.fillMaxSize(),
            contentAlignment  = Alignment.Center,
        ) {
            // Semi-dark overlay around the viewfinder square
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
            // Transparent cut-out with corner borders
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color.Transparent)
                    .border(
                        width  = 3.dp,
                        color  = Color.White.copy(alpha = 0.9f),
                        shape  = RoundedCornerShape(12.dp),
                    ),
            )
        }

        // Hint text
        Text(
            text       = s.joinTripScanHint,
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center,
            modifier   = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .padding(horizontal = 32.dp),
        )
    }
}

// ── Permission denied state ───────────────────────────────────────────────────

@Composable
private fun PermissionDeniedContent(message: String) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text      = message,
            color     = Color.White,
            textAlign = TextAlign.Center,
            fontSize  = 16.sp,
        )
    }
}
