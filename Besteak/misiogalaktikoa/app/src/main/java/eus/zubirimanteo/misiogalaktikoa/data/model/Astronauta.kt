package eus.zubirimanteo.misiogalaktikoa.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "astronautak")
data class Astronauta(
    @PrimaryKey(autoGenerate = true)
    val astronautaId: Int = 0,
    val izena: String,
    val espezialitatea: String
)