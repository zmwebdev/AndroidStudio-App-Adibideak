package eus.zubirimanteo.astrolog

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eus.zubirimanteo.astrolog.data.AstroDatabase
import eus.zubirimanteo.astrolog.data.Observation
import eus.zubirimanteo.astrolog.databinding.ActivityMainBinding
import eus.zubirimanteo.astrolog.ui.ObservationAdapter
import eus.zubirimanteo.astrolog.ui.ObservationDetailDialog
import eus.zubirimanteo.astrolog.ui.ObservationViewModel
import eus.zubirimanteo.astrolog.ui.ObservationViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ObservationViewModel
    private lateinit var adapter: ObservationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tresna-barra ezarri eta menua kudeatzeko
        setSupportActionBar(binding.topAppBar)

        // 1. Datu-basea eta DAO-a hasieratzea
        val database = AstroDatabase.getDatabase(this)
        val dao = database.observationDao()

        // 2. ViewModel Factory erabiliz ViewModel instantziatu
        val factory = ObservationViewModelFactory(dao)
        viewModel = ViewModelProvider(this, factory)[ObservationViewModel::class.java]

        // 3. RecyclerView eta Adapter hasieratu
        adapter = ObservationAdapter { observation ->
            // Klik-ekintza: Behaketa baten xehetasunak erakusteko edo editatzeko.
            // KLIK EKINTZA BERRIA: Behaketa Editatu/Ezabatu dialogoa erakutsi
            showObservationDetailDialog(observation)
            // Adibide gisa: Toast mezu bat erakutsiko dugu.
            //Toast.makeText(this, "${observation.celestialBody} behaketa hautatu da. ID: ${observation.id}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 4. READ (Irakurri): Datu guztiak behatu eta UI-a eguneratu
        lifecycleScope.launch {
            viewModel.allObservations.collect { observations ->
                adapter.submitList(observations)
                // Zerrenda hutsik dagoen egiaztatu
                if (observations.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }

        // 5. CREATE (Sortu): FAB botoiak behaketa berri bat sortuko du formularioa erakutsiz
        binding.fabAdd.setOnClickListener {
            // Sortu Behaketa objektu huts bat (ID = 0, Room-ek autogeneratuko duela adieraziz)
            val newObservation = Observation(
                celestialBody = "",
                date = "",
                notes = "",
                scopeSetting = ""
            )
            // Dialogoa erakutsi sortzeko moduan
            showObservationDetailDialog(newObservation)
        }

//        // 5. CREATE (Sortu): FAB botoiak behaketa berri bat sortuko du adibidez
//        binding.fabAdd.setOnClickListener {
//            // Datu berriak sortu (demo gisa)
//            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
//            val currentDate = format.format(Date())
//
//            val celestialBodies = listOf("Ilargia", "Artizarra", "Marte", "Jupiter", "Saturno")
//            val randomBody = celestialBodies.random()
//
//            val randomNotes = "Behaketa zail bat izan zen hodei batzuk zirela eta, baina ${randomBody} oso argi ikusi zen."
//            val randomScope = "Dobson 10''"
//
//            viewModel.insertObservation(randomBody, currentDate, randomNotes, randomScope)
//            Toast.makeText(this, "${randomBody} behaketa berria gehitu da!", Toast.LENGTH_SHORT).show()
//        }
    }

    /**
     * Behaketa Editatu/Ezabatu dialogoa erakusten du.
     */
    private fun showObservationDetailDialog(observation: Observation) {
        ObservationDetailDialog(observation, viewModel).show(supportFragmentManager, ObservationDetailDialog.TAG)
    }

    /**
     * Menuaren baliabidea kargatu Top AppBar-ean
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Menuaren elementuen klik-ekintzak kudeatu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_all -> {
                // Konfirmazio mezu baten ordez zuzenean deitu (UI sinplea mantentzeko)
                viewModel.deleteAllObservations()
                Toast.makeText(this, "Behaketa guztiak ezabatu dira.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Ezarpenak orria laster gehituko da.", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}