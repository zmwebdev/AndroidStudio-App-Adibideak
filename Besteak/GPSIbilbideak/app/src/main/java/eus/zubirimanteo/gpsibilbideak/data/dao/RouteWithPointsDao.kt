package eus.zubirimanteo.gpsibilbideak.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import eus.zubirimanteo.gpsibilbideak.data.relations.RouteWithPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteWithPointsDao {
    //@Transaction
    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun getRouteWithPoints(routeId: Long): Flow<RouteWithPoints?>
}