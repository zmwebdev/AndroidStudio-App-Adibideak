package eus.zubirimanteo.misiogalaktikoa.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import eus.zubirimanteo.misiogalaktikoa.data.model.Astronauta
import kotlinx.coroutines.flow.Flow

@Dao
interface AstronautaDAO {
    // Astronauta bat sortzeko metodoa
    @Insert
    suspend fun sortuAstronauta(astronauta: Astronauta) // 'suspend' funtzioa da, datu-baseko eragiketak hari nagusitik kanpo egiteko

    // Astronauta guztiak berreskuratu
    @Query("SELECT * FROM astronautak ORDER BY astronautaId ASC")
    fun getAstronautaGuztiak(): Flow<List<Astronauta>>

}