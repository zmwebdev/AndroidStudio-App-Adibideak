package eus.zubirimanteo.gpsibilbideak.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val distance: Double,
    val duration: Double,
)