# Tutorial: Building a QR Scanner with Jetpack Compose, CameraX and ML Kit

Claude: "I want a tutorial (step-by-step) to create an Android Jetpack Compose app using Camera (For example to read QR). Students knows Jetpack Compose, but they have no idea about how to integrate Camera and other sensors"

https://claude.ai/share/4a24614e-88a1-42b5-9e44-b282148b63fa


*Audience: students who already know Jetpack Compose (state, side effects, composables) but have never used the camera or hardware sensors on Android.*

## What we will build

A small app that:

1. Asks the user for camera permission.
2. Shows a live camera preview inside a Compose screen.
3. Detects QR codes in real time and shows the decoded text.
4. (Bonus) Reads a second sensor (the accelerometer) using the same "hardware as lifecycle-aware state" pattern.

**Time:** about 90 minutes. **Requirements:** Android Studio (latest stable), a physical device is strongly recommended (emulator cameras work but are awkward for QR codes), minSdk 24 or higher.

## 0. The big picture (read this first)

Compose has no built-in camera composable. Camera is a *hardware resource* with a lifecycle: it must be opened when the screen is visible and released when it is not. To work with it we combine three pieces:

| Piece | Library | Responsibility |
| --- | --- | --- |
| Runtime permission | `ActivityResultContracts` | Ask the user for `CAMERA` |
| Camera access | **CameraX** | Open the camera, deliver a preview and image frames |
| Image understanding | **ML Kit Barcode Scanning** | Decode QR codes from frames |

CameraX works with **use cases** that you bind to a lifecycle:

- `Preview`: shows the live image on screen.
- `ImageAnalysis`: gives you each frame so you can process it (our QR detection).
- `ImageCapture`: takes photos (not needed today).

The data flow is:

```
Camera sensor → CameraX → Preview use case → PreviewView (on screen)
                        ↘ ImageAnalysis use case → QrAnalyzer → ML Kit → String → Compose state → UI
```

The key mental model for Compose: **the camera is a side effect; the decoded value is state.** The camera pipeline lives in effects (`LaunchedEffect`, `DisposableEffect`), and results flow back into the UI through a callback that updates state.

## 1. Create the project

1. New Project → **Empty Activity** (the Compose template).
2. Name: `QrScannerDemo`, package e.g. `com.example.qrscanner`.
3. Run it once to verify the template works.

## 2. Add dependencies

In `app/build.gradle.kts`, inside `dependencies { }`:

```kotlin
val cameraxVersion = "1.4.2" // check for the latest stable version
implementation("androidx.camera:camera-core:$cameraxVersion")
implementation("androidx.camera:camera-camera2:$cameraxVersion")
implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
implementation("androidx.camera:camera-view:$cameraxVersion")

// ML Kit: bundled model, works offline, no Google Play services download needed
implementation("com.google.mlkit:barcode-scanning:17.3.0")
```

Sync the project.

> **Why `camera-view`?** It provides `PreviewView`, a classic Android `View` that renders the camera image efficiently. We embed it in Compose with `AndroidView`. This is the most widely used and most stable approach.

> **Bundled vs. unbundled ML Kit:** the dependency above bundles the model inside your APK (about 2 to 3 MB). It works immediately and offline. The alternative `play-services-mlkit-barcode-scanning` downloads the model via Google Play services.

## 3. Declare the permission

In `AndroidManifest.xml`, **above** `<application>`:

```xml
<uses-permission android:name="android.permission.CAMERA" />

<!-- Allows installing the app on devices without a camera; we handle that case in code if needed -->
<uses-feature android:name="android.hardware.camera.any" android:required="false" />
```

**Concept:** `CAMERA` is a *dangerous* permission. Declaring it in the manifest is not enough: since Android 6 you must also **ask the user at runtime**. That is the next step.

## 4. Request the permission from Compose

Create a file `CameraPermission.kt`. We build a small composable that *gates* its content behind the permission.

```kotlin
package com.example.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        denied = !granted
    }

    // Ask automatically the first time the screen appears
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (denied) "Camera permission is needed to scan QR codes."
                else "Requesting camera permission…"
            )
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant permission")
            }
        }
    }
}
```

**What to notice**

- `rememberLauncherForActivityResult` is the Compose-friendly way to use the Activity Result API. No `onRequestPermissionsResult` needed.
- Permission status is **state** (`hasPermission`). When it changes, Compose recomposes and shows the camera.
- Production apps should also handle "Don't ask again" by sending the user to the app settings. We skip that to keep the tutorial focused.

## 5. Write the QR analyzer (the "brain")

CameraX delivers each camera frame to an `ImageAnalysis.Analyzer`. Create `QrAnalyzer.kt`:

```kotlin
package com.example.qrscanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // only QR: faster
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // rotationDegrees tells ML Kit how the sensor image is rotated relative to the display
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let(onQrDetected)
            }
            .addOnCompleteListener {
                // VERY IMPORTANT: always close the frame, or CameraX stops sending new ones
                imageProxy.close()
            }
    }

    fun close() = scanner.close()
}
```

**Key ideas**

- `analyze()` is called for **every frame**, on a background thread (we choose which one in the next step).
- You **must** call `imageProxy.close()`. Forgetting it is the number one bug: the preview keeps running but analysis silently stops after a few frames.
- ML Kit works asynchronously, so we close the frame when the task completes.
- `FORMAT_QR_CODE` restricts the work to QR codes. Remove the line to scan every barcode type.

## 6. The camera composable (the "bridge" between CameraX and Compose)

Create `QrScannerView.kt`. This is the most important file, so read the comments carefully.

```kotlin
package com.example.qrscanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
```

**Walkthrough**

| Piece | Why it is there |
| --- | --- |
| `PreviewView` + `AndroidView` | Compose can host any Android `View`. |
| `remember { ... }` | Prevents recreating the view, executor and analyzer on every recomposition. |
| `LaunchedEffect(lifecycleOwner)` | Camera startup is a suspending side effect. `awaitInstance` suspends until the camera provider is ready. |
| `bindToLifecycle(...)` | **The magic line.** CameraX opens the camera when the lifecycle is STARTED and closes it when it is STOPPED. You never call `open()` or `close()` yourself. |
| `STRATEGY_KEEP_ONLY_LATEST` | If ML Kit is busy, skip frames instead of building a queue. |
| `rememberUpdatedState` | Avoids capturing a stale callback inside long-lived objects. A classic Compose gotcha. |
| `DisposableEffect` + `onDispose` | Cleanup for things that are not managed by Compose. |

## 7. Put it all together on a screen

Create `ScannerScreen.kt`:

```kotlin
package com.example.qrscanner

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
```

Update `MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ScannerScreen()
            }
        }
    }
}
```

## 8. Run and test

1. Run on a physical device (USB debugging enabled).
2. Accept the camera permission.
3. Point the camera at any QR code (generate one at any online QR generator, or display one on your monitor).
4. A dialog with the decoded text should appear.

**Troubleshooting checklist**

- *Black preview:* did you grant the permission? Is `bindToLifecycle` reached (add a `Log.d`)?
- *Detects once then never again:* `imageProxy.close()` is missing or not reached.
- *Dialog appears many times:* the analyzer fires on every frame; keep the `if (scannedValue == null)` guard.
- *Rotated or mirrored results:* check that you pass `rotationDegrees` to `InputImage`.
- *Emulator shows a fake scene:* configure the virtual scene or use a webcam in the AVD settings.

## 9. Exercises

**Easy**

1. Show the result in a `Text` at the bottom of the screen instead of a dialog.
2. Make the result clickable: if it starts with `http`, open it with an `Intent.ACTION_VIEW`.

**Medium**

3. Draw a square overlay (a `Canvas` or a bordered `Box`) in the center to guide the user.
4. Keep a history list of scanned values (avoiding duplicates) in a `ViewModel` using a `StateFlow`.
5. Add a torch button: after binding, keep the returned `Camera` object and call `camera.cameraControl.enableTorch(true)`.

**Advanced**

6. Move the camera logic into a `ViewModel` and expose a `UiState` (`Scanning`, `Result(value)`, `Error`).
7. Use `BarcodeScannerOptions` with all formats and display the `valueType` (URL, WiFi, contact...).
8. Show bounding boxes on the detected code using `barcode.boundingBox`, converting image coordinates to view coordinates.

## 10. Bonus: other sensors with the same pattern

Camera is only one kind of hardware. Motion, light, proximity or step sensors use `SensorManager`. The pattern is identical: **register a listener when the screen is active, unregister when it leaves, expose values as state.**

Example: a composable that shows live accelerometer values.

```kotlin
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun AccelerometerReadout() {
    val context = LocalContext.current
    var xyz by remember { mutableStateOf(Triple(0f, 0f, 0f)) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                xyz = Triple(event.values[0], event.values[1], event.values[2])
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // null means the device has no such sensor
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    Text("x=%.2f  y=%.2f  z=%.2f".format(xyz.first, xyz.second, xyz.third))
}
```

**Compare it with the camera**

|  | Camera (CameraX) | Sensor (SensorManager) |
| --- | --- | --- |
| Needs permission? | Yes, `CAMERA` | Accelerometer, light, gyroscope: no. Step counter and activity recognition: yes |
| Start/stop | Automatic via `bindToLifecycle` | Manual with `registerListener` / `unregisterListener` |
| Compose tool | `LaunchedEffect` + `DisposableEffect` | `DisposableEffect` |
| Output | Frames → analyzer → state | Events → state |

**Suggested exercise:** build a "spirit level" that tilts a `Box` according to the accelerometer values, or a light meter using `Sensor.TYPE_LIGHT`.

## 11. Good practices summary

- Treat hardware as **side effects** and results as **state**.
- Always release what you open (`DisposableEffect`, `unbindAll`, `close()`).
- Always check that a sensor exists (`getDefaultSensor` can return `null`).
- Do heavy work off the main thread (analyzer executor).
- Request permissions at the moment they are needed and explain why.
- Test on real devices: emulators do not reproduce real camera or sensor behaviour well.

## 12. Where to go next

- Official CameraX documentation: developer.android.com/media/camera/camerax
- ML Kit Barcode Scanning guide: developers.google.com/ml-kit/vision/barcode-scanning/android
- Android Sensors overview: developer.android.com/develop/sensors-and-location/sensors/sensors-overview
- CameraX also offers a Compose-native viewfinder (`camera-compose`) that replaces `PreviewView` + `AndroidView`. Once students master this tutorial, comparing both approaches is a great follow-up.

> **Note for the instructor:** library versions change often. Before class, check the latest stable versions of CameraX and ML Kit on their release pages and update the Gradle snippet accordingly.
