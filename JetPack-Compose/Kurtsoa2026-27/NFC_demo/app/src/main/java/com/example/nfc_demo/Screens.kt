package com.example.nfc_demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nfc_demo.ui.theme.NFC_demoTheme

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

@Composable
fun TagExplorerApp(vm: NfcViewModel, onOpenNfcSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    TagExplorerContent(
        state = state,
        onOpenNfcSettings = onOpenNfcSettings,
        onClearTag = vm::clearTag,
        onArmWrite = vm::armWrite,
        onCancelWrite = vm::cancelWrite
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagExplorerContent(
    state: NfcUiState,
    onOpenNfcSettings: () -> Unit,
    onClearTag: () -> Unit,
    onArmWrite: (String) -> Unit,
    onCancelWrite: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Combine sensor + NFC: shake the phone to clear the last tag
    val accel by rememberSensorValues(android.hardware.Sensor.TYPE_ACCELEROMETER)
    val g = accel?.let {
        kotlin.math.sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2]) /
                android.hardware.SensorManager.GRAVITY_EARTH
    } ?: 1f
    val shaking = g > 2.5f
    LaunchedEffect(shaking) { if (shaking) onClearTag() }

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
                1 -> WriteScreen(state, onArm = onArmWrite, onCancel = onCancelWrite)
                2 -> SensorsScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TagExplorerAppPreview() {
    NFC_demoTheme {
        TagExplorerContent(
            state = NfcUiState(
                status = NfcStatus.READY,
                lastTag = TagInfo(
                    uid = "04:A2:B3:C4:D5:E6:F7",
                    technologies = listOf("NfcA", "MifareUltralight", "Ndef"),
                    records = listOf("Text: Hello NFC"),
                    maxSize = 512,
                    writable = true
                )
            ),
            onOpenNfcSettings = {},
            onClearTag = {},
            onArmWrite = {},
            onCancelWrite = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TagCardPreview() {
    NFC_demoTheme {
        TagCard(
            tag = TagInfo(
                uid = "04:A2:B3:C4:D5:E6:F7",
                technologies = listOf("NfcA", "MifareUltralight", "Ndef"),
                records = listOf("Text: Hello NFC", "URI: https://developer.android.com"),
                maxSize = 512,
                writable = true
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WriteScreenPreview() {
    NFC_demoTheme {
        WriteScreen(
            state = NfcUiState(),
            onArm = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SensorsScreenPreview() {
    NFC_demoTheme {
        SensorsScreen()
    }
}

