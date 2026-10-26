package com.example.nfc_demo

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

fun readTag(tag: Tag): TagInfo {
    val uid = tag.id.joinToString(":") { "%02X".format(it) }
    val techs = tag.techList.map { it.substringAfterLast('.') } // e.g. NfcA, Ndef

    val ndef = Ndef.get(tag)  // null if the tag has no NDEF data
    val records = ndef?.cachedNdefMessage?.records?.map(::describe) ?: emptyList()

    return TagInfo(uid, techs, records, ndef?.maxSize, ndef?.isWritable)
}

private fun describe(r: NdefRecord): String = when {
    r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_TEXT) ->
        "Text: " + parseText(r)
    r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_URI) ->
        "URI: " + r.toUri()
    else -> "Other record (TNF=" + r.tnf + ", " + r.payload.size + " bytes)"
}

// Text record payload: [status byte][language code][text]
// status byte: bit 7 = encoding (0 = UTF-8), bits 0-5 = language code length
private fun parseText(r: NdefRecord): String {
    val p = r.payload
    val status = p[0].toInt()
    val charset = if ((status and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
    val langLen = status and 0x3F
    return String(p, 1 + langLen, p.size - 1 - langLen, charset)
}

fun writeText(tag: Tag, text: String) {
    val message = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", text)))

    val ndef = Ndef.get(tag)
    if (ndef != null) {                      // tag already formatted as NDEF
        ndef.use {
            it.connect()
            require(it.isWritable) { "Tag is read-only" }
            require(message.toByteArray().size <= it.maxSize) { "Text too long for this tag" }
            it.writeNdefMessage(message)
        }
    } else {                                 // blank tag: format and write in one step
        val formatable = NdefFormatable.get(tag)
            ?: throw IllegalStateException("Tag does not support NDEF")
        formatable.use {
            it.connect()
            it.format(message)
        }
    }
}