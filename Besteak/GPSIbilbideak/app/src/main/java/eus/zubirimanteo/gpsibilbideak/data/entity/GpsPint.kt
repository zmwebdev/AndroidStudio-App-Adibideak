package eus.zubirimanteo.gpsibilbideak.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = "gps_points",
//    foreignKeys = [
//        ForeignKey(
//            entity = Route::class,
//            parentColumns = ["id"],
//            childColumns = ["routeId"],
//            onDelete = ForeignKey.CASCADE
//        )
//    ],
//    indices = [Index(value = ["routeId"])]
)
data class GpsPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

