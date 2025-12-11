package eus.zubirimanteo.gpsibilbideak.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import eus.zubirimanteo.gpsibilbideak.data.entity.GpsPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: GpsPoint) : Long

    @Update
    suspend fun update(point: GpsPoint)

    @Delete
    suspend fun delete(point: GpsPoint)

    @Query("SELECT * FROM gps_points WHERE routeId = :routeId ORDER BY timestamp ASC")
    fun getByRoute(routeId: Long): Flow<List<GpsPoint>>

    @Query("DELETE FROM gps_points WHERE routeId = :routeId")
    suspend fun deleteByRoute(routeId: Long)
}