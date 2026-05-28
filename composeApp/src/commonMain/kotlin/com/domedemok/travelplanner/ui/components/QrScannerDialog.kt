package com.domedemok.travelplanner.ui.components

import androidx.compose.runtime.Composable

/**
 * Full-screen QR-code scanner dialog.
 *
 * Calls [onCodeScanned] with the extracted 6-character join code once a
 * valid TravelPlanner QR is detected, or [onDismiss] when the user cancels.
 *
 * Platform actuals:
 *  - androidMain : CameraX + ML Kit barcode scanning
 *  - jsMain      : BarcodeDetector Web API (Chrome/Edge) via DOM overlay
 */
@Composable
expect fun QrScannerDialog(
    onCodeScanned: (String) -> Unit,
    onDismiss:     () -> Unit,
)
