package com.example.qrscannerdemo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ScannerScreen() {
    var scannedValue by remember { mutableStateOf<String?>(null) }

    CameraPermissionGate {
        Box(Modifier.fillMaxSize()) {
            QrScannerView(
                modifier = Modifier.fillMaxSize(),
                onQrDetected = { value ->
                    // Ignore new detections while a result is already being shown
                    if (scannedValue == null) scannedValue = value
                }
            )
        }

        scannedValue?.let { value ->
            AlertDialog(
                onDismissRequest = { scannedValue = null },
                title = { Text("QR detected") },
                text = { Text(value) },
                confirmButton = {
                    TextButton(onClick = { scannedValue = null }) { Text("Scan again") }
                }
            )
        }
    }
}