package com.example.sensorlab

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