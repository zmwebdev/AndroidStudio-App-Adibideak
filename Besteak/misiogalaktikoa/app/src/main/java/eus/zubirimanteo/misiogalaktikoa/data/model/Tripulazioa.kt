package eus.zubirimanteo.misiogalaktikoa.data.model

import androidx.room.Entity

// N-M
// https://developer.android.com/training/data-storage/room/relationships/many-to-many

@Entity(
    tableName = "tripulazioa",
    primaryKeys = ["misioaId", "astronautaId"]
)
data class Tripulazioa(
    val misioaId: Int,  // Gako Arrotza Misioa-rentzat
    val astronautaId: Int // Gako Arrotza Astronauta-rentzat
)