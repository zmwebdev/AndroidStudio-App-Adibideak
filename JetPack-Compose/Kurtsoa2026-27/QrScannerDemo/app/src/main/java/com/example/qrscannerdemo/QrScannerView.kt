package com.example.qrscannerdemo

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun QrScannerView(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // The classic View that draws the camera image. remember = create it once.
    val previewView = remember { PreviewView(context) }

    // Frames are analysed on a background thread, never on the main thread
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // If the caller passes a new lambda, we use the latest one without restarting the camera
    val latestCallback by rememberUpdatedState(onQrDetected)

    val analyzer = remember { QrAnalyzer { value -> latestCallback(value) } }

    // 1) Start the camera and bind it to the lifecycle
    LaunchedEffect(lifecycleOwner) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            // Drop old frames if the analyzer is slower than the camera
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }

        try {
            cameraProvider.unbindAll() // clean slate
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2) Release resources when this composable leaves the screen
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            analyzer.close()
        }
    }

    // 3) Show the preview inside Compose
    AndroidView(factory = { previewView }, modifier = modifier)
}