package com.example.nfc_demo

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