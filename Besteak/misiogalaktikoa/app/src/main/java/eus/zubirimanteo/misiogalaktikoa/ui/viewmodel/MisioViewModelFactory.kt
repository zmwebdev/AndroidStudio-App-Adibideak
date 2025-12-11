package eus.zubirimanteo.misiogalaktikoa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eus.zubirimanteo.misiogalaktikoa.data.repository.MisioRepository

class MisioViewModelFactory(private val repository: MisioRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MisioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MisioViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}