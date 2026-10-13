# Tutorial: GPS and Sensors in Jetpack Compose

Claude Prompt: "I want a tutorial (step-by-step) to create an Android Jetpack Compose app using GPS. Students knows Jetpack Compose, but they have no idea about how to integrate GPS and other sensors."

*A step-by-step guide for students who already know Compose but are new to location and device sensors.*

## What you will build

A single-screen app called **SensorLab** that shows:

- Your live GPS position (latitude, longitude, accuracy, speed)
- A compass heading that rotates an arrow on screen
- Live accelerometer values (x, y, z)

Along the way you will learn the three ideas that matter for every sensor app:

1. **Permissions** must be requested at runtime, from inside Compose.
2. **Sensors are streams.** Both GPS and hardware sensors push values over time, so we wrap them in Kotlin `Flow`.
3. **Lifecycle matters.** Sensors drain battery, so they must start when the screen is visible and stop when it is not.

**Prerequisites:** Android Studio (recent version), Kotlin basics, Compose basics (state, `@Composable`, `remember`), and a basic idea of `ViewModel`. A real phone is best, but the emulator works too (see Step 9).

## The big picture

```
GPS chip / sensor hardware
        |
   Android system  (FusedLocationProviderClient / SensorManager)
        |
   Repository  (wraps callbacks into a Flow)
        |
   ViewModel   (turns Flow into StateFlow)
        |
   Composable  (collectAsStateWithLifecycle -> UI)
```

Each layer has one job. The UI never talks to the GPS directly.

## Step 1: Create the project and add dependencies

1. In Android Studio choose **New Project > Empty Activity** (this template already uses Compose). Name it `SensorLab`.
2. Open `app/build.gradle.kts` and add these dependencies:

```kotlin
dependencies {
    // GPS: Fused Location Provider (part of Google Play services)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Lifecycle-aware state collection in Compose + ViewModel integration
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
}
```

3. Click **Sync Now**.

> Why *Fused* Location Provider? It combines GPS, Wi-Fi and cell data and picks the best source automatically. It is simpler and more battery friendly than the old `LocationManager` API. Check the official docs for the latest version numbers.

## Step 2: Declare permissions in the manifest

Open `AndroidManifest.xml` and add these lines **above** the `<application>` tag:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

- **Coarse** = approximate location (a few hundred metres).
- **Fine** = precise GPS.

On Android 12 and later the user can grant only approximate location even if you ask for precise, so your app must tolerate both.

The accelerometer and compass sensors need **no permission**. (Some sensors do, for example the step counter needs `ACTIVITY_RECOGNITION`.)

## Step 3: Ask for permission from Compose

Declaring a permission is not enough. Location is a *dangerous* permission, so the user must approve it while the app is running.

Create `PermissionGate.kt`. This composable shows your content only when permission is granted, and otherwise shows a button to request it:

```kotlin
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

@Composable
fun LocationPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasLocationPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                  result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    if (granted) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("This app needs your location to show GPS data.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }) { Text("Grant permission") }
        }
    }
}
```

**Key idea:** the permission result is just *state* (`granted`). When it changes, Compose recomposes and shows the real screen. No callbacks into the Activity needed.

## Step 4: Your first GPS reading (one-shot)

Before building the full solution, let students see the simplest possible call. `FusedLocationProviderClient.lastLocation` returns the most recent known position once:

```kotlin
val client = LocationServices.getFusedLocationProviderClient(context)

@SuppressLint("MissingPermission")
fun readOnce() {
    client.lastLocation.addOnSuccessListener { location ->
        // location can be null (no recent fix available)
        println("Lat=${location?.latitude}, Lon=${location?.longitude}")
    }
}
```

Two lessons here:

- The result can be **null**. Always handle it.
- `lastLocation` may be old. For a live position we need *updates*, which is the next step.

## Step 5: Continuous GPS updates as a Flow

GPS delivers new positions through a callback. Callbacks do not fit well with Compose, but a `Flow` does. Kotlin's `callbackFlow` converts one into the other.

Create `LocationRepository.kt`:

```kotlin
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationRepository(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // the UI only calls this after permission is granted
    fun locationUpdates(intervalMs: Long = 2_000L): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        // Runs when nobody collects the Flow any more: STOP the GPS here.
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
```

Read it in three parts:

1. **`LocationRequest`** says how often we want updates and how accurate they should be. `PRIORITY_HIGH_ACCURACY` uses the GPS chip (most precise, most battery). `PRIORITY_BALANCED_POWER_ACCURACY` is a cheaper alternative.
2. **`LocationCallback`** receives positions and pushes them into the Flow with `trySend`.
3. **`awaitClose`** is the cleanup block. This is the most important line in the file: without it the GPS would keep running forever.

## Step 6: Sensors as a Flow

Hardware sensors work the same way, through `SensorManager`. Create `SensorRepository.kt`:

```kotlin
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class SensorRepository(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Generic helper: emits the raw values array of any sensor type. */
    private fun sensorValues(
        type: Int,
        delay: Int = SensorManager.SENSOR_DELAY_UI
    ): Flow<FloatArray> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(type)
        if (sensor == null) {          // not every phone has every sensor!
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values.clone()) // clone: the system reuses the array
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, delay)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /** Accelerometer: acceleration in m/s² on x, y, z (includes gravity). */
    fun accelerometer(): Flow<FloatArray> = sensorValues(Sensor.TYPE_ACCELEROMETER)

    /** Compass heading in degrees: 0 = North, 90 = East, 180 = South, 270 = West. */
    fun heading(): Flow<Float> =
        sensorValues(Sensor.TYPE_ROTATION_VECTOR).map { values ->
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            (azimuthDeg + 360f) % 360f
        }
}
```

Things to point out to students:

- **Sampling rate** (`SENSOR_DELAY_UI`, `_NORMAL`, `_GAME`, `_FASTEST`): faster means more battery use. `UI` is fine for showing values on screen.
- **Sensor may not exist.** Always check for `null`.
- **Rotation vector** is a *fused* (software) sensor that combines accelerometer, gyroscope and magnetometer. It is the easiest way to build a compass. The math (`getRotationMatrixFromVector` and `getOrientation`) is provided by Android, so students do not need to implement it.
- The compass reports the angle relative to **magnetic** North, which differs slightly from true North.

## Step 7: The ViewModel

The ViewModel turns our Flows into `StateFlow`s that the UI can observe. Create `SensorsViewModel.kt`:

```kotlin
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SensorsViewModel(app: Application) : AndroidViewModel(app) {

    private val locationRepo = LocationRepository(app)
    private val sensorRepo = SensorRepository(app)

    // WhileSubscribed(5000): keep the sensor running for 5 s after the UI stops
    // observing (survives screen rotation), then shut it down to save battery.
    private val policy = SharingStarted.WhileSubscribed(5_000)

    val location: StateFlow<Location?> =
        locationRepo.locationUpdates().stateIn(viewModelScope, policy, null)

    val heading: StateFlow<Float> =
        sensorRepo.heading().stateIn(viewModelScope, policy, 0f)

    val acceleration: StateFlow<FloatArray> =
        sensorRepo.accelerometer().stateIn(viewModelScope, policy, floatArrayOf(0f, 0f, 0f))
}
```

`stateIn` with `WhileSubscribed` is the secret ingredient: **nobody collecting = sensors off**. That is exactly the behaviour we want.

## Step 8: The Compose UI

Create `SensorScreen.kt`:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SensorScreen(vm: SensorsViewModel = viewModel()) {
    // collectAsStateWithLifecycle stops collecting when the app goes to the background
    val location by vm.location.collectAsStateWithLifecycle()
    val heading by vm.heading.collectAsStateWithLifecycle()
    val accel by vm.acceleration.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SensorLab", style = MaterialTheme.typography.headlineMedium)

        // --- GPS card ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("GPS", style = MaterialTheme.typography.titleMedium)
                val loc = location
                if (loc == null) {
                    Text("Waiting for a GPS fix...")
                } else {
                    Text("Latitude:  %.6f".format(loc.latitude))
                    Text("Longitude: %.6f".format(loc.longitude))
                    Text("Accuracy:  %.1f m".format(loc.accuracy))
                    Text("Speed:     %.1f m/s".format(loc.speed))
                }
            }
        }

        // --- Compass card ---
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Compass: %.0f°".format(heading), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "↑",
                    style = MaterialTheme.typography.displayLarge,
                    // Rotate opposite to the heading so the arrow keeps pointing North
                    modifier = Modifier.rotate(-heading)
                )
                Text("The arrow points to magnetic North")
            }
        }

        // --- Accelerometer card ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Accelerometer (m/s²)", style = MaterialTheme.typography.titleMedium)
                Text("x: %.2f".format(accel[0]))
                Text("y: %.2f".format(accel[1]))
                Text("z: %.2f".format(accel[2]))
            }
        }
    }
}
```

Finally, wire everything together in `MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LocationPermissionGate {   // the screen (and the GPS) only exists after permission
                    SensorScreen()
                }
            }
        }
    }
}
```

Because `SensorScreen()` is only composed inside the gate, `locationUpdates()` is never started without permission.

**Why `collectAsStateWithLifecycle` and not `collectAsState`?** The plain version keeps collecting while the app is in the background, so the GPS would stay on. The lifecycle-aware version stops at `onStop` and resumes at `onStart`. Students should run the app, press Home, and check that the sensors stop (see Step 9).

## Step 9: Test it

### On a real phone

1. Connect the phone with USB debugging enabled and press **Run**.
2. Grant the location permission. Go near a window or outside. The first GPS fix can take 10 to 60 seconds.
3. Rotate the phone: the arrow should keep pointing North, and the accelerometer values should change when you tilt it.

### On the emulator

Use an emulator image that includes **Google Play services** (a *Google APIs* or *Google Play* image), otherwise the fused provider will not work.

1. Click the **...** (Extended controls) button in the emulator toolbar.
2. **Location** tab: set a point (or a route, then press *Play route*) to simulate GPS movement.
3. **Virtual sensors** tab: use *Device pose* or *Additional sensors* to rotate the virtual phone and see the accelerometer and compass change.

### Check the lifecycle

Press Home while the app is running, then open Logcat or the battery tools. The location callback should stop shortly after (after the 5 second grace period). Come back to the app: updates resume.

## Common problems and how to fix them

- **Location stays "Waiting for a GPS fix..."**: you are indoors, the device's location switch is off, or the emulator has no location set. Open the Location tab and send a position.
- **App crashes with `SecurityException`**: the code requested location before permission was granted. Make sure everything is inside `LocationPermissionGate`.
- **Compass is jittery or wrong**: sensors are noisy. Move the phone in a figure-8 to calibrate, and try smoothing (see Exercise 3).
- **Sensor value is always 0 on the emulator**: the emulator only simulates some sensors. Use the Virtual sensors panel.
- **Background tracking does not work**: foreground-only location, as in this tutorial, stops when the app is not visible. Tracking in the background needs `ACCESS_BACKGROUND_LOCATION` plus a foreground service. That is a more advanced topic with stricter Play Store rules.

## Concepts students should remember

- Sensors and GPS are **asynchronous streams of data**. Wrap them in `Flow`.
- Always **unregister** listeners (`awaitClose`). Forgetting this is the number one bug and battery killer.
- Tie sensor activity to the **UI lifecycle** (`WhileSubscribed` + `collectAsStateWithLifecycle`).
- Always handle **missing permission**, **missing sensor** and **null location**.
- Higher accuracy and higher sampling rate cost **battery**. Choose the lowest that satisfies your use case.

## Exercises

1. **Distance tracker (easy):** keep the previous `Location` and add up the distance with `previous.distanceTo(current)`. Show the total in metres.
2. **Start/Stop button (easy):** add a button that toggles whether the ViewModel collects the location flow (hint: a `MutableStateFlow<Boolean>` and `flatMapLatest`).
3. **Smooth compass (medium):** apply a low-pass filter, for example `smoothed = smoothed + 0.1f * (new - smoothed)`. Take care at the 359° to 0° wrap-around.
4. **Shake detector (medium):** compute the magnitude `sqrt(x² + y² + z²)` from the accelerometer and show a message when it exceeds about 15 m/s².
5. **Step counter (medium):** add `Sensor.TYPE_STEP_COUNTER`. It needs the `ACTIVITY_RECOGNITION` permission on Android 10+, so extend the permission gate.
6. **Map (advanced):** show the position on a map using the Maps Compose library or osmdroid, and draw the route as a polyline.

## Further reading

- Android Developers: *Location and context*, *Request location permissions*, *Sensors overview*
- Android Developers: *Consuming flows safely in Jetpack Compose*
- Kotlin docs: `callbackFlow` and `stateIn`
