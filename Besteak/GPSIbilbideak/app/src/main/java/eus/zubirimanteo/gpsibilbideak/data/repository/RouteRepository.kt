package eus.zubirimanteo.gpsibilbideak.data.repository

import eus.zubirimanteo.gpsibilbideak.data.AppDatabase
import eus.zubirimanteo.gpsibilbideak.data.entity.GpsPoint
import eus.zubirimanteo.gpsibilbideak.data.entity.Route
import kotlinx.coroutines.flow.Flow

class RouteRepository(private val db: AppDatabase) {

    fun getRoutes(): Flow<List<Route>> = db.routeDao().getAll()

    fun getRoute(routeId: Long): Flow<Route?> = db.routeDao().getById(routeId)

    fun getRouteWithPoints(routeId: Long) = db.routeWithPointsDao().getRouteWithPoints(routeId)

    fun getPoints(routeId: Long): Flow<List<GpsPoint>> = db.gpsPointDao().getByRoute(routeId)

    suspend fun createRoute(route: Route): Long = db.routeDao().insert(route)

    suspend fun updateRoute(route: Route) = db.routeDao().update(route)

    suspend fun deleteRoute(route: Route) {
        db.gpsPointDao().deleteByRoute(route.id)
        db.routeDao().delete(route)
    }

    suspend fun addPoint(point: GpsPoint): Long = db.gpsPointDao().insert(point)

    suspend fun updatePoint(point: GpsPoint) = db.gpsPointDao().update(point)

    suspend fun deletePoint(point: GpsPoint) = db.gpsPointDao().delete(point)
}