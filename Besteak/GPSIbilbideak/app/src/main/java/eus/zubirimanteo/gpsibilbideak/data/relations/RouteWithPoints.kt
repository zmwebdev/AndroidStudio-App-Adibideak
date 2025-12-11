package eus.zubirimanteo.gpsibilbideak.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import eus.zubirimanteo.gpsibilbideak.data.entity.GpsPoint
import eus.zubirimanteo.gpsibilbideak.data.entity.Route

data class RouteWithPoints(
    @Embedded val route: Route,
    @Relation(
        parentColumn = "id",
        entityColumn = "routeId"
    )
    val points: List<GpsPoint>
)