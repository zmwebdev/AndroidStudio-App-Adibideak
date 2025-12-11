package eus.zubirimanteo.gpsibilbideak.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import eus.zubirimanteo.gpsibilbideak.data.entity.Route
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: Route) : Long

    @Update
    suspend fun update(route: Route)

    @Delete
    suspend fun delete(route: Route)

    @Query("SELECT * FROM routes ORDER BY name DESC")
    fun getAll(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Route?>
}