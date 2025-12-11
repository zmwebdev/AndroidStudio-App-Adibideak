package eus.zubirimanteo.astrolog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eus.zubirimanteo.astrolog.data.Observation
import eus.zubirimanteo.astrolog.data.ObservationDao
import kotlinx.coroutines.launch

/**
 * [Observation] entitatearen UI egoera mantentzeko eta datu-baseko eragiketak kudeatzeko ViewModel-a.
 */
class ObservationViewModel(private val observationDao: ObservationDao) : ViewModel() {

    // READ: Behaketa guztiak Lortu (Flow gisa, UI-k behatzeko)
    val allObservations = observationDao.getAllObservations()

    /**
     * CREATE (Sortu): Behaketa berri bat sartzen du.
     */
    fun insertObservation(celestialBody: String, date: String, notes: String, scopeSetting: String) {
        val newObservation = Observation(
            celestialBody = celestialBody,
            date = date,
            notes = notes,
            scopeSetting = scopeSetting
        )
        // Datu-baseko eragiketak ViewModeleko Coroutine esparruaren barruan exekutatu
        viewModelScope.launch {
            observationDao.insert(newObservation)
        }
    }

    /**
     * UPDATE (Eguneratu): Behaketa bat eguneratzen du.
     */
    fun updateObservation(id: Int, celestialBody: String, date: String, notes: String, scopeSetting: String) {
        val updatedObservation = Observation(
            id = id,
            celestialBody = celestialBody,
            date = date,
            notes = notes,
            scopeSetting = scopeSetting
        )
        viewModelScope.launch {
            observationDao.update(updatedObservation)
        }
    }

    /**
     * DELETE (Ezabatu): Behaketa bat ezabatzen du.
     */
    fun deleteObservation(observation: Observation) {
        viewModelScope.launch {
            observationDao.delete(observation)
        }
    }

    /**
     * DELETE ALL (Guztiak Ezabatu): Behaketa guztiak ezabatzen ditu.
     */
    fun deleteAllObservations() {
        viewModelScope.launch {
            observationDao.deleteAll()
        }
    }
}

/**
 * ViewModel honetarako Factory klasea (Menpekotasun Injekzio sinplea).
 */
class ObservationViewModelFactory(private val observationDao: ObservationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ObservationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ObservationViewModel(observationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}