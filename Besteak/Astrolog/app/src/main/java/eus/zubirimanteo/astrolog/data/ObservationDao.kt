package eus.zubirimanteo.astrolog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Behaketen datu-baseko sarbide objektua (DAO). CRUD eragiketak definitzen ditu.
 */

@Dao
interface ObservationDao {

    // 1. READ (Irakurri) - Eragiketa: Behaketa guztiak lortu
    // Flow erabiliz datu-baseko aldaketa guztietan automatikoki eguneratuko da.
    @Query("SELECT * FROM observations ORDER BY observation_date DESC")
    fun getAllObservations(): Flow<List<Observation>>

    // 2. READ (Irakurri) - Eragiketa: Behaketa bakarra ID bidez lortu
    @Query("SELECT * FROM observations WHERE id = :id")
    fun getObservationById(id: Int): Flow<Observation>

    // 3. CREATE (Sortu) - Eragiketa: Behaketa berri bat sartu
    // OnConflictStrategy.REPLACE erabiltzen da gatazkarik izanez gero existitzen den erregistroa ordezkatzeko.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(observation: Observation)

    // 4. UPDATE (Eguneratu) - Eragiketa: Existitzen den behaketa eguneratu
    // Suspend funtzioa da Coroutine baten barruan exekutatzeko.
    @Update
    suspend fun update(observation: Observation)

    // 5. DELETE (Ezabatu) - Eragiketa: Behaketa bat ezabatu
    // Suspend funtzioa da Coroutine baten barruan exekutatzeko.
    @Delete
    suspend fun delete(observation: Observation)

    // 6. DELETE ALL (Guztiak Ezabatu) - Eragiketa gehigarria
    @Query("DELETE FROM observations")
    suspend fun deleteAll()
}