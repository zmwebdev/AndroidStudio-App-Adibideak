package com.example.nfc_demo

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