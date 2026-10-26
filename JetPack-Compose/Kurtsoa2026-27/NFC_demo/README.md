# NFC and Sensors in Jetpack Compose: Step-by-Step Tutorial

Claude: https://claude.ai/chat/39b1a36d-a902-4eda-a26a-ded076026567?artifact=bb7a1050-1ae3-4639-9aff-dfa4c6a47f61

*Audience: students who already know Jetpack Compose, state hoisting, ViewModel and StateFlow, but have never used NFC or hardware sensors.*

## 1. What we will build

**Tag Explorer**, a three-tab app:

1. **Read**: tap an NFC tag and see its UID, technologies and NDEF records (text and URIs).
2. **Write**: type a text, press *Arm*, tap a tag and the text is written to it.
3. **Sensors**: live accelerometer and light-sensor values.

Finally we **combine both worlds**: shaking the phone clears the last scanned tag.

**Learning goals**

- Understand how Android talks to NFC tags (reader mode, NDEF).
- Bridge callback-based Android APIs (NFC, sensors) into Compose state.
- Respect lifecycle so hardware is only used while the app is visible.

**Requirements**: Android Studio, a **physical** Android phone with NFC (the emulator has no NFC), a few blank NFC tags (NTAG213/215/216 are cheap and perfect), minSdk 26 or higher.

## 2. Theory in five minutes

### NFC in one paragraph

NFC (Near Field Communication) works at about 4 cm. The phone emits a field and a passive tag (no battery) answers. Android supports three modes: **reader/writer** (phone reads or writes tags, our topic), **peer-to-peer** (removed in modern Android) and **card emulation** (HCE, the phone behaves like a card; advanced, out of scope).

### NDEF

Most tags store data as **NDEF** (NFC Data Exchange Format): a *message* made of one or more *records*. Common records are **Text** and **URI**. Android gives us `NdefMessage` and `NdefRecord` classes to build and parse them.

### Two ways to receive tags

| Approach | When to use |
| --- | --- |
| **Reader mode** (`enableReaderMode`) | App is in the foreground and wants to handle tags itself. Simple, modern, recommended. **We use this.** |
| **Intent filters** (`NDEF_DISCOVERED`) | Launch the app when a tag is scanned from outside. |

### Sensors in one paragraph

`SensorManager` gives access to hardware sensors. You register a `SensorEventListener` and Android calls `onSensorChanged` many times per second. You **must unregister** or you drain the battery. Sensors like the accelerometer or light sensor need **no permission**.

## 3. Step 1: Create the project and dependencies

1. New Project, *Empty Activity* (Compose), package `com.example.tagexplorer`, minSdk 26.
2. In `app/build.gradle.kts` add the lifecycle-aware Compose collector (keep the version consistent with the other lifecycle libraries in your template):

```kotlin
dependencies {
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended")
}
```

Sync Gradle.

## 4. Step 2: Manifest

NFC needs a permission. Declaring the hardware as *not required* lets the app install on phones without NFC, and we show a friendly message instead.

```xml
<uses-permission android:name="android.permission.NFC" />

<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

Place both inside `<manifest>`, before `<application>`.

> Note: `NFC` is a *normal* permission. It is granted at install time, no runtime dialog.

## 5. Step 3: The data model

Create `NfcModels.kt`. The UI will only see plain Kotlin data, never `Tag` or `Ndef` objects.

```kotlin
package com.example.tagexplorer

enum class NfcStatus { UNSUPPORTED, DISABLED, READY }

data class TagInfo(
    val uid: String,
    val technologies: List<String>,
    val records: List<String>,
    val maxSize: Int?,
    val writable: Boolean?
)

data class NfcUiState(
    val status: NfcStatus = NfcStatus.READY,
    val lastTag: TagInfo? = null,
    val pendingWrite: String? = null, // non-null means "write mode is armed"
    val message: String? = null
)
```

## 6. Step 4: Reading and writing tags

Create `NfcTagIo.kt`. These are plain functions, easy to explain and test.

```kotlin
package com.example.tagexplorer

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

fun readTag(tag: Tag): TagInfo {
    val uid = tag.id.joinToString(":") { "%02X".format(it) }
    val techs = tag.techList.map { it.substringAfterLast('.') } // e.g. NfcA, Ndef

    val ndef = Ndef.get(tag)  // null if the tag has no NDEF data
    val records = ndef?.cachedNdefMessage?.records?.map(::describe) ?: emptyList()

    return TagInfo(uid, techs, records, ndef?.maxSize, ndef?.isWritable)
}

private fun describe(r: NdefRecord): String = when {
    r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_TEXT) ->
        "Text: " + parseText(r)
    r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_URI) ->
        "URI: " + r.toUri()
    else -> "Other record (TNF=" + r.tnf + ", " + r.payload.size + " bytes)"
}

// Text record payload: [status byte][language code][text]
// status byte: bit 7 = encoding (0 = UTF-8), bits 0-5 = language code length
private fun parseText(r: NdefRecord): String {
    val p = r.payload
    val status = p[0].toInt()
    val charset = if ((status and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
    val langLen = status and 0x3F
    return String(p, 1 + langLen, p.size - 1 - langLen, charset)
}

fun writeText(tag: Tag, text: String) {
    val message = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", text)))

    val ndef = Ndef.get(tag)
    if (ndef != null) {                      // tag already formatted as NDEF
        ndef.use {
            it.connect()
            require(it.isWritable) { "Tag is read-only" }
            require(message.toByteArray().size <= it.maxSize) { "Text too long for this tag" }
            it.writeNdefMessage(message)
        }
    } else {                                 // blank tag: format and write in one step
        val formatable = NdefFormatable.get(tag)
            ?: throw IllegalStateException("Tag does not support NDEF")
        formatable.use {
            it.connect()
            it.format(message)
        }
    }
}
```

**Key ideas to explain in class**

- A `Tag` is just the raw discovery object. To talk to it you ask for a *technology* (`Ndef`, `NdefFormatable`, `MifareClassic`, ...) with `Tech.get(tag)`. If the tag doesn't support it, you get `null`.
- Reading uses `cachedNdefMessage` (already read by Android when the tag was discovered). Writing needs `connect()`, and the connection is closed by `use`.
- Writing is **blocking I/O**. Reader mode calls us on a background thread, so this is fine, but never do it on the main thread.

## 7. Step 5: The ViewModel

Create `NfcViewModel.kt`. The ViewModel owns the state and decides what a scanned tag means: *read* or *write*, depending on whether write mode is armed.

```kotlin
package com.example.tagexplorer

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NfcViewModel : ViewModel() {
    private val _state = MutableStateFlow(NfcUiState())
    val state: StateFlow<NfcUiState> = _state.asStateFlow()

    fun setStatus(status: NfcStatus) = _state.update { it.copy(status = status) }

    fun armWrite(text: String) = _state.update {
        it.copy(pendingWrite = text, message = "Hold a tag against the back of the phone...")
    }

    fun cancelWrite() = _state.update { it.copy(pendingWrite = null, message = null) }

    fun clearTag() = _state.update { it.copy(lastTag = null) }

    // Called from a background thread by the NFC system
    fun onTag(tag: Tag) {
        val pending = _state.value.pendingWrite
        if (pending != null) {
            val result = runCatching { writeText(tag, pending) }
            _state.update { s ->
                s.copy(
                    pendingWrite = null,
                    message = result.fold(
                        onSuccess = { "Written successfully" },
                        onFailure = { e -> "Write failed: " + e.message }
                    )
                )
            }
        } else {
            runCatching { readTag(tag) }
                .onSuccess { info -> _state.update { s -> s.copy(lastTag = info, message = null) } }
                .onFailure { e -> _state.update { s -> s.copy(message = "Read failed: " + e.message) } }
        }
    }
}
```

`MutableStateFlow.update` is thread-safe, so it is safe to call from the NFC background thread.

## 8. Step 6: The Activity (the only place that touches the NFC adapter)

Open the generated MainActivity.kt and replace its contents. enableReaderMode needs an Activity, so the Activity owns the adapter and forwards tags to the ViewModel. It calls TagExplorerApp, the root composable we write in Step 10, so the project will not compile until you reach that step.

```kotlin
package com.example.tagexplorer

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val vm: NfcViewModel by viewModels()
    private var adapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NfcAdapter.getDefaultAdapter(this)   // null = no NFC hardware
        setContent {
            MaterialTheme {
                TagExplorerApp(
                    vm = vm,
                    onOpenNfcSettings = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val a = adapter
        vm.setStatus(
            when {
                a == null -> NfcStatus.UNSUPPORTED
                !a.isEnabled -> NfcStatus.DISABLED
                else -> NfcStatus.READY
            }
        )
        a?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        adapter?.disableReaderMode(this)    // always release it
    }

    // Called on a background thread every time a tag is discovered
    override fun onTagDiscovered(tag: Tag) = vm.onTag(tag)
}
```

**Why `onResume`/`onPause`?** Reader mode only works while the Activity is in the foreground. Enabling in `onResume` and disabling in `onPause` is the standard pattern. We also re-check `isEnabled` in `onResume` because the user may have just come back from the NFC settings screen.

## 9. Step 7: Compose screens for NFC

Create `Screens.kt`. Everything is stateless and driven by `NfcUiState`.

```kotlin
package com.example.tagexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReadScreen(state: NfcUiState, onOpenNfcSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state.status) {
            NfcStatus.UNSUPPORTED -> Text("This device has no NFC.")
            NfcStatus.DISABLED -> {
                Text("NFC is turned off.")
                Button(onClick = onOpenNfcSettings) { Text("Open NFC settings") }
            }
            NfcStatus.READY -> {
                val tag = state.lastTag
                if (tag == null) Text("Hold an NFC tag against the back of your phone.")
                else TagCard(tag)
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun TagCard(tag: TagInfo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("UID: " + tag.uid, style = MaterialTheme.typography.titleMedium)
            Text("Technologies: " + tag.technologies.joinToString())
            tag.maxSize?.let { Text("Capacity: " + it + " bytes") }
            tag.writable?.let { Text("Writable: " + it) }
            HorizontalDivider()
            if (tag.records.isEmpty()) Text("No NDEF records (blank or non-NDEF tag)")
            else tag.records.forEach { Text(it) }
        }
    }
}

@Composable
fun WriteScreen(state: NfcUiState, onArm: (String) -> Unit, onCancel: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("Hello NFC") }
    val armed = state.pendingWrite != null

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text to write") },
            enabled = !armed,
            modifier = Modifier.fillMaxWidth()
        )
        if (armed) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        } else {
            Button(onClick = { onArm(text) }, enabled = text.isNotBlank()) {
                Text("Arm write mode")
            }
        }
        state.message?.let { Text(it) }
    }
}
```

## 10. Step 8: From callbacks to Compose, the sensor flow

This is the most important idea of the second half: **turn any Android listener API into a `Flow`**, then collect it in Compose. Create `SensorFlow.kt`.

```kotlin
package com.example.tagexplorer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

fun SensorManager.sensorFlow(
    type: Int,
    delay: Int = SensorManager.SENSOR_DELAY_UI
): Flow<FloatArray> = callbackFlow {
    val sensor = getDefaultSensor(type)
    if (sensor == null) {            // this phone lacks the sensor
        close()
        return@callbackFlow
    }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            trySend(event.values.clone())   // Android reuses the array, so copy it
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    registerListener(listener, sensor, delay)
    awaitClose { unregisterListener(listener) }   // runs when collection stops
}

@Composable
fun rememberSensorValues(type: Int): State<FloatArray?> {
    val context = LocalContext.current
    val flow = remember(type) {
        context.getSystemService<SensorManager>()?.sensorFlow(type) ?: emptyFlow()
    }
    // Collection stops when the app goes to the background, so the sensor is
    // automatically unregistered by awaitClose. No manual lifecycle code needed.
    return flow.collectAsStateWithLifecycle(initialValue = null)
}
```

**Explain step by step**

1. `callbackFlow` wraps a callback API. `trySend` pushes values into the flow.
2. `awaitClose` is **mandatory**: it is where we unregister the listener.
3. `collectAsStateWithLifecycle` (instead of `collectAsState`) cancels collection when the lifecycle drops below STARTED. This is what saves battery.
4. `event.values` is reused by the framework. Without `.clone()` you would see all your values mutate to the latest reading.

**Sampling rates**: `SENSOR_DELAY_NORMAL` (about 5 Hz), `UI` (about 16 Hz), `GAME` (about 50 Hz), `FASTEST`. Pick the slowest one that works. Above 200 Hz on Android 12+ you need the `HIGH_SAMPLING_RATE_SENSORS` permission.

## 11. Step 9: Sensors screen

Add to `Screens.kt`:

```kotlin
@Composable
fun SensorsScreen() {
    val accel by rememberSensorValues(android.hardware.Sensor.TYPE_ACCELEROMETER)
    val light by rememberSensorValues(android.hardware.Sensor.TYPE_LIGHT)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Accelerometer (m/s2)", style = MaterialTheme.typography.titleMedium)
                val a = accel
                if (a == null) Text("Waiting for data or not available")
                else {
                    Text("x = " + "%.2f".format(a[0]))
                    Text("y = " + "%.2f".format(a[1]))
                    Text("z = " + "%.2f".format(a[2]))
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Light sensor", style = MaterialTheme.typography.titleMedium)
                Text(light?.let { "%.0f lux".format(it[0]) } ?: "Waiting for data or not available")
            }
        }
    }
}
```

Try it: lay the phone flat (z is about 9.8), tilt it, cover the light sensor with your finger.

## 12. Step 10: The app shell, and combining NFC with a sensor

Add this to `Screens.kt`. A bottom navigation bar with three tabs, plus the shake gesture.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagExplorerApp(vm: NfcViewModel, onOpenNfcSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Combine sensor + NFC: shake the phone to clear the last tag
    val accel by rememberSensorValues(android.hardware.Sensor.TYPE_ACCELEROMETER)
    val g = accel?.let {
        kotlin.math.sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2]) /
            android.hardware.SensorManager.GRAVITY_EARTH
    } ?: 1f
    val shaking = g > 2.5f
    LaunchedEffect(shaking) { if (shaking) vm.clearTag() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tag Explorer") }) },
        bottomBar = {
            NavigationBar {
                listOf("Read", "Write", "Sensors").forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {},
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> ReadScreen(state, onOpenNfcSettings)
                1 -> WriteScreen(state, onArm = vm::armWrite, onCancel = vm::cancelWrite)
                2 -> SensorsScreen()
            }
        }
    }
}
```

Add the missing imports (`collectAsStateWithLifecycle`, `ExperimentalMaterial3Api`, `mutableIntStateOf`). Android Studio's *Alt+Enter* will offer them.

**Discussion**: why is the shake threshold 2.5 g and not 1 g? (Gravity alone gives about 1 g when the phone is still. A shake briefly exceeds that. Real apps usually filter noise and require several peaks.)

## 13. Step 11: Run and test

1. Enable **Developer options** and **USB debugging** on the phone and plug it in. The emulator cannot be used for NFC.
2. Make sure **NFC is on** in system settings.
3. Run the app. Open **Write**, type a text, press *Arm write mode*, and touch a tag to the back of the phone (the antenna is usually near the camera or centre).
4. Go to **Read** and touch the same tag. You should see your text record.
5. Scan a transit card, a hotel key or a product tag: you will see other technologies (e.g. `MifareClassic`, `IsoDep`) and no NDEF records. Many are encrypted and cannot be read by apps.
6. Open **Sensors**, then shake the phone while a tag is shown on **Read**.

## 14. Common pitfalls

| Symptom | Cause and fix |
| --- | --- |
| Nothing happens when scanning | NFC off, screen locked, or wrong spot on the phone. Move the tag slowly across the back. |
| `TagLostException` while writing | The tag moved. Hold still until you see the result. |
| App crashes touching UI from the NFC callback | Never touch Compose or Views directly. Update a `StateFlow`, like we did. |
| Sensor values freeze at the last number | Forgot `.clone()` or never unregistered the listener. |
| Battery drain | Listener not unregistered, or sampling rate too high. |
| Tag shows no records | Blank tag or non-NDEF. Writing with `NdefFormatable` fixes blank ones. |
| Works on one phone, not another | NFC antenna position and sensor availability vary. Always handle `null` sensors and `null` adapters. |

## 15. Exercises for students

1. **Easy**: write a *URI* record (`NdefRecord.createUri("https://...")`) and open it in the browser when a tag with a URI is read.
2. **Easy**: vibrate (`VibratorManager`) when a tag is read.
3. **Medium**: keep a **history list** of scanned tags in the ViewModel and show it with `LazyColumn`.
4. **Medium**: add a **tilt ball** on a `Canvas` that moves with accelerometer x and y.
5. **Medium**: add the **gyroscope** or **proximity** sensor. Use `sensorFlow` unchanged: only the type changes.
6. **Hard**: launch the app from a scanned tag using an `NDEF_DISCOVERED` intent filter and read the intent's `EXTRA_NDEF_MESSAGES`.
7. **Hard**: use `callbackFlow` to wrap another callback API (e.g. `LocationManager`) the same way.

## 16. Where to go next

- Official docs: *NFC basics*, *Advanced NFC* and *Sensors Overview* on developer.android.com.
- **Host-based card emulation (HCE)**: make the phone behave like a tag.
- Other sensors that need permissions: `ACTIVITY_RECOGNITION` (step counter), `BODY_SENSORS` (heart rate).
- Testing: abstract `Tag` access behind an interface so you can unit-test the ViewModel with fakes.

## 17. Summary of the architecture

- **Activity**: owns `NfcAdapter`, enables and disables reader mode with the lifecycle, forwards tags.
- **ViewModel**: single `StateFlow<NfcUiState>`, decides read versus write.
- **Helpers**: `readTag` and `writeText` isolate all NFC API details.
- **`sensorFlow`**: converts any `SensorManager` listener into a `Flow`.
- **Compose**: stateless screens that only render state and send events up.

The pattern *hardware callback to Flow or StateFlow to Compose state* is the same for NFC, sensors, Bluetooth, location and more.
