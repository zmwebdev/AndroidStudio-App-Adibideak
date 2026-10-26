package com.example.nfc_demo

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