package com.example.sensorlab

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensorlab.ui.theme.SensorLabTheme

@Composable
fun SensorScreen(vm: SensorsViewModel = viewModel()) {
    // collectAsStateWithLifecycle stops collecting when the app goes to the background
    val location by vm.location.collectAsStateWithLifecycle()
    val heading by vm.heading.collectAsStateWithLifecycle()
    val accel by vm.acceleration.collectAsStateWithLifecycle()

    SensorScreenContent(
        location = location,
        heading = heading,
        accel = accel
    )
}

@Composable
fun SensorScreenContent(
    location: Location?,
    heading: Float,
    accel: FloatArray
) {
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

@Preview(showBackground = true)
@Composable
fun SensorScreenPreview() {
    val sampleLocation = Location("sample").apply {
        latitude = 43.3183
        longitude = -1.9812
        accuracy = 5.0f
        speed = 1.2f
    }
    SensorLabTheme {
        SensorScreenContent(
            location = sampleLocation,
            heading = 45f,
            accel = floatArrayOf(0.1f, 9.81f, 0.2f)
        )
    }
}