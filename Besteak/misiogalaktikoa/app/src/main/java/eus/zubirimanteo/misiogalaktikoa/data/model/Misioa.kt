package eus.zubirimanteo.misiogalaktikoa.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "misioak")
data class Misioa(
    @PrimaryKey(autoGenerate = true)
    val misioaId: Int = 0,
    val izena: String,
    val hasieraData: Long // Long mota erabiltzen dugu data gordetzeko (Unix denbora). String baino hobea da ordenatzeko eta alderatzeko.
)