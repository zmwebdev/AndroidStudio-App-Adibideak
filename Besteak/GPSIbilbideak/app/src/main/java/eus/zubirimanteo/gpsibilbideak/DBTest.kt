package eus.zubirimanteo.gpsibilbideak

import android.util.Log
import eus.zubirimanteo.gpsibilbideak.data.AppDatabase
import eus.zubirimanteo.gpsibilbideak.data.entity.GpsPoint
import eus.zubirimanteo.gpsibilbideak.data.entity.Route
import kotlinx.coroutines.flow.first

class DBTest {
    suspend fun runDbOperation(db: AppDatabase) {
        // Lortu DAO-ak datu-basearen instantziatik
        val routeDao = db.routeDao()
        val gpsPointDao = db.gpsPointDao() // Lortu GpsPoint-en DAO-a

        // 1. DATUAK SORTU ETA SARTU (CREATE) - Ibilbideak
        Log.d("DBTest", "Ibilbideak sortzen...")
        val routeId1 = routeDao.insert(Route(name = "Donostia-Orio", description = "Donostia-Orio Kostatik", distance = 10.5, duration = 2.5))
        val routeId2 = routeDao.insert(Route(name = "Orio-Asteasu", description = "Oriotik Asteasura", distance = 16.5, duration = 4.5))
        Log.d("DBTest", "Sartutako ibilbideak: ID $routeId1 eta $routeId2")

        // 2. GPS PUNTUAK SORTU ETA SARTU (CREATE) - Puntuak
        Log.d("DBTest", "GPS puntuak gehitzen...")

        // Lehen ibilbidearentzat (Donostia-Orio) puntuak
        gpsPointDao.insert(GpsPoint(routeId = routeId1, latitude = 43.3183, longitude = -1.9812)) // Donostia
        gpsPointDao.insert(GpsPoint(routeId = routeId1, latitude = 43.3033, longitude = -2.0626)) // Igeldo
        gpsPointDao.insert(GpsPoint(routeId = routeId1, latitude = 43.2820, longitude = -2.1280)) // Orio

        // Bigarren ibilbidearentzat (Orio-Asteasu) puntuak
        gpsPointDao.insert(GpsPoint(routeId = routeId2, latitude = 43.2820, longitude = -2.1280)) // Orio
        gpsPointDao.insert(GpsPoint(routeId = routeId2, latitude = 43.2325, longitude = -2.0919)) // Aia
        gpsPointDao.insert(GpsPoint(routeId = routeId2, latitude = 43.1944, longitude = -2.0658)) // Asteasu

        Log.d("DBTest", "GPS puntuak behar bezala gehitu dira.")


        // 3. DATUAK ALDATU (UPDATE) - Ibilbide bat
        Log.d("DBTest", "Ibilbide bat aldatzen...")
        val lehenIbilbidea = routeDao.getById(routeId1).first()
        if (lehenIbilbidea != null) {
            val ibilbideAldatua = lehenIbilbidea.copy(name = "Donostia-Orio (Aldatuta)")
            routeDao.update(ibilbideAldatua)
            Log.d("DBTest", "ID $routeId1 duen ibilbidea aldatu da.")
        }

        // 4. DATUAK EZABATU (DELETE) - Ibilbide bat
        Log.d("DBTest", "Ibilbide bat ezabatzen...")
        val bigarrenIbilbidea = routeDao.getById(routeId2).first()
        if (bigarrenIbilbidea != null) {
            routeDao.delete(bigarrenIbilbidea)
            // Room-ek ON DELETE CASCADE erabiltzen ez badu, puntuak eskuz ezabatu beharko lirateke.
            // Zure kasuan, suposatuko dugu 'foreignKeys' ondo konfiguratuta dagoela.
            Log.d("DBTest", "ID $routeId2 duen ibilbidea ezabatu da (eta bere puntuak ere ezabatu beharko lirateke).")
        }

        // 5. AZKENEKO EGIAZTAPENA
        Log.d("DBTest", "------ AZKEN EGOERA ------")
        val geratzenDirenak = routeDao.getAll().first()
        Log.d("DBTest", "GERATZEN DIREN IBILBIDEAK: $geratzenDirenak")

        // Egiaztatu lehen ibilbideari lotutako puntuak
        if(geratzenDirenak.isNotEmpty()) {
            val lehenIbilbidearenPuntuak = gpsPointDao.getByRoute(geratzenDirenak.first().id).first()
            Log.d("DBTest", "Lehen ibilbideari lotutako puntuak: $lehenIbilbidearenPuntuak")
        }

        // Egiaztatu ea bigarren ibilbideko puntuak ezabatu diren
        val ezabatutakoIbilbidearenPuntuak = gpsPointDao.getByRoute(routeId2).first()
        Log.d("DBTest", "Ezabatutako ibilbidearen (ID $routeId2) puntuak: $ezabatutakoIbilbidearenPuntuak (hutsik egon beharko luke)")

        Log.d("DBTest", "Datu-basearen eragiketak amaituta.")
    }
}