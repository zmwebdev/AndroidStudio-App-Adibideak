package eus.zubirimanteo.astrolog.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import eus.zubirimanteo.astrolog.data.Observation
import eus.zubirimanteo.astrolog.databinding.DialogObservationDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [ObservationDetailDialog] Behaketa baten xehetasunak erakutsi, editatu eta ezabatzeko dialogoa.
 * [ObservationViewModel] zuzenean pasatzen diogu eragiketak egiteko.
 */
class ObservationDetailDialog(
    private val observation: Observation,
    private val viewModel: ObservationViewModel
) : DialogFragment() {

    private var _binding: DialogObservationDetailBinding? = null
    // ViewBinding-en delegatu bat sortu
    private val binding get() = _binding!!

    // ID 0 bada, behaketa berria da (Sortu modua).
    private val isNewObservation = observation.id == 0

    // Material Design 3 estiloko dialogoa erabiltzeko
    override fun getTheme() = com.google.android.material.R.style.Theme_Material3_DayNight_Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogObservationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI pertsonalizazioa: Sortu vs Editatu modua
        if (isNewObservation) {
            // Sortu modua
            binding.titleText.text = "Behaketa Berria Sortu"
            binding.deleteButton.visibility = View.GONE // Ezkutatu ezabatu botoia sortzean
            binding.updateButton.text = "Sortu Behaketa"

            // Kargatu data lehenetsia
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentDate = format.format(Date())
            binding.dateEdit.setText(currentDate)

        } else {
            // Editatu modua
            binding.titleText.text = "Behaketa Editatu/Ezabatu"
            binding.deleteButton.visibility = View.VISIBLE
            binding.updateButton.text = "Gorde / Eguneratu"

            // Datu kargatu sarrera-eremuetan
            binding.celestialBodyEdit.setText(observation.celestialBody)
            binding.dateEdit.setText(observation.date)
            binding.scopeSettingEdit.setText(observation.scopeSetting)
            binding.notesEdit.setText(observation.notes)
        }

        // 1. SAVE/UPDATE Logika
        binding.updateButton.setOnClickListener {
            saveOrUpdateObservation()
        }

        // 2. DELETE (Ezabatu) Logika (bakarrik Editatu moduan ikusgai)
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    /**
     * Sarrera-eremuetatik datuak bildu eta ViewModel-aren bidez Gorde edo Eguneratu.
     */
    private fun saveOrUpdateObservation() {
        val newCelestialBody = binding.celestialBodyEdit.text.toString().trim()
        val newDate = binding.dateEdit.text.toString().trim()
        val newScope = binding.scopeSettingEdit.text.toString().trim()
        val newNotes = binding.notesEdit.text.toString().trim()

        if (newCelestialBody.isEmpty() || newDate.isEmpty()) {
            Toast.makeText(requireContext(), "Zeruko Gorputza eta Data ezin dira hutsik egon.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isNewObservation) {
            // CREATE: Behaketa berria sortu
            viewModel.insertObservation(newCelestialBody, newDate, newNotes, newScope)
            Toast.makeText(requireContext(), "${newCelestialBody} behaketa sortu da.", Toast.LENGTH_SHORT).show()
        } else {
            // UPDATE: Existitzen den behaketa eguneratu
            viewModel.updateObservation(
                id = observation.id,
                celestialBody = newCelestialBody,
                date = newDate,
                notes = newNotes,
                scopeSetting = newScope
            )
            Toast.makeText(requireContext(), "${newCelestialBody} behaketa eguneratu da.", Toast.LENGTH_SHORT).show()
        }

        dismiss() // Dialogoa itxi
    }

    /**
     * Ezabatzeko baieztapen-dialogoa erakutsi.
     */
    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Behaketa Ezabatu?")
            .setMessage("${observation.celestialBody} behaketa betirako ezabatu nahi duzu?")
            .setNegativeButton("Utzi") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Ezabatu") { _, _ ->
                // Ezabatu behaketa Room-en
                viewModel.deleteObservation(observation)
                Toast.makeText(requireContext(), "Behaketa ezabatu da.", Toast.LENGTH_SHORT).show()
                dismiss() // Behaketa ezabatzean ere dialogoa itxi
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ObservationDetailDialog"
    }
}