package com.example.sensorlab

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