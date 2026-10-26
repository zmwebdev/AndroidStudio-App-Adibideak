package com.example.nfc_demo

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val vm: NfcViewModel by viewModels()
    private var adapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NfcAdapter.getDefaultAdapter(this)   // null = no NFC hardware
        setContent {
            MaterialTheme {
                TagExplorerApp(
                    vm = vm,
                    onOpenNfcSettings = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val a = adapter
        vm.setStatus(
            when {
                a == null -> NfcStatus.UNSUPPORTED
                !a.isEnabled -> NfcStatus.DISABLED
                else -> NfcStatus.READY
            }
        )
        a?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        adapter?.disableReaderMode(this)    // always release it
    }

    // Called on a background thread every time a tag is discovered
    override fun onTagDiscovered(tag: Tag) = vm.onTag(tag)
}