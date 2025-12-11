package eus.zubirimanteo.misiogalaktikoa.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import eus.zubirimanteo.misiogalaktikoa.data.model.MisioAstronauta
import eus.zubirimanteo.misiogalaktikoa.data.model.Misioa
import kotlinx.coroutines.flow.Flow

@Dao
interface MisioDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sortuMisioa(misioa: Misioa)

    @Query("SELECT * FROM misioak ORDER BY hasieraData ASC")
    fun getMisioGuztiak(): Flow<List<Misioa>>

    @Update
    suspend fun eguneratuMisioa(misioa: Misioa)

    @Delete
    suspend fun ezabatuMisioa(misioa: Misioa)

    // Misio guztiak eta bakoitzaren tripulazioa lortzeko
    @Transaction  // Eragiketa bakarra dela zihurtzatzeko
    @Query("SELECT * FROM misioak ORDER BY hasieraData ASC")
    fun getMisioakTripulazioarekin(): Flow<List<MisioAstronauta>>
}