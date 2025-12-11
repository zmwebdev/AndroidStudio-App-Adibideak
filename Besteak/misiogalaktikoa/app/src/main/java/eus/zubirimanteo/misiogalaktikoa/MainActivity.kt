package eus.zubirimanteo.misiogalaktikoa

import MisioAdapter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eus.zubirimanteo.misiogalaktikoa.data.db.MisioGalaktikoenDB
import eus.zubirimanteo.misiogalaktikoa.data.model.Misioa
import eus.zubirimanteo.misiogalaktikoa.data.repository.MisioRepository
import eus.zubirimanteo.misiogalaktikoa.databinding.ActivityMainBinding
import eus.zubirimanteo.misiogalaktikoa.ui.viewmodel.MisioViewModel
import eus.zubirimanteo.misiogalaktikoa.ui.viewmodel.MisioViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // DB hasieratu
    private val viewModel: MisioViewModel by viewModels {
        MisioViewModelFactory(MisioRepository(MisioGalaktikoenDB.getInstance(this, lifecycleScope).misioDao()))
    }

    private lateinit var misioAdapter: MisioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Adapter-a eta RecyclerView-a konfiguratu
        misioAdapter = MisioAdapter { misioa -> /* ... klik logika ... */ } // Adapter-aren instantzia
        binding.misioZerrenda.adapter = misioAdapter
        // LayoutManager XML-an dagoenez, ez dugu hemen berriro ezarri behar.

        // 2. ViewModel-eko datuak behatu (StateFlow)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // misioGuztiak StateFlow-a behatu
                viewModel.misioGuztiak.collect { misioZerrenda ->
                    // 3. Adapter-ari datu berriak bidali
                    misioAdapter.submitList(misioZerrenda)
                }
            }
        }

        binding.fabGehituMisioa.setOnClickListener {
            val misioBerria = Misioa(
                izena = "MArtitzera bidaia berria",
                hasieraData = System.currentTimeMillis()  // uneko data
            )

            viewModel.sortuMisioa(misioBerria)
        }
    }
}