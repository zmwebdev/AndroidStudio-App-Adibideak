package eus.zubirimanteo.misiogalaktikoa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eus.zubirimanteo.misiogalaktikoa.data.model.Misioa
import eus.zubirimanteo.misiogalaktikoa.data.repository.MisioRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class MisioViewModel(
    private val repository: MisioRepository
) : ViewModel() {
    val misioGuztiak: StateFlow<List<Misioa>> = repository.misioGuztiak.stateIn(
        scope = viewModelScope,  // https://developer.android.com/topic/libraries/architecture/coroutines#viewmodelscope
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun sortuMisioa(misioa: Misioa) {
        viewModelScope.launch {
            repository.sortuMisioa(misioa)
        }
    }

    fun eguneratuMisioa(misioa: Misioa) {
        viewModelScope.launch {
            repository.eguneratuMisioa(misioa)
        }
    }

    fun ezabatuMisioa(misioa: Misioa) {
        viewModelScope.launch {
            repository.ezabatuMisioa(misioa)
        }
    }
}

    