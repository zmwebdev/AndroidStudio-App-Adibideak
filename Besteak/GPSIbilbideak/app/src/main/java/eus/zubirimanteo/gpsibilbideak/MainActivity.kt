package eus.zubirimanteo.gpsibilbideak

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import eus.zubirimanteo.gpsibilbideak.data.AppDatabase
import eus.zubirimanteo.gpsibilbideak.data.entity.GpsPoint
import eus.zubirimanteo.gpsibilbideak.data.entity.Route
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            val db = AppDatabase.get(applicationContext)
            val dbTest = DBTest()
            dbTest.runDbOperation(db)
        }
    }
}