package eus.zubirimanteo.astrolog.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [Observation] entitatea (taula). Behatutako zeruko gorputz bakoitzeko datuak gordetzen ditu.
 * @param id Erregistroaren gako nagusia. Auto-sortua.
 * @param celestialBody Behatutako gorputza (adibidez, "Marte", "Jupiterren Ilargia").
 * @param date Behaketa egin zen data eta ordua (testu gisa gordeta).
 * @param notes Ohar pertsonalak edo behaketaren deskribapena.
 * @param scopeSetting Teleskopioaren ezarpenak edo erabilitako tresneria.
 */
@Entity(tableName = "observations")
data class Observation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "celestial_body")
    val celestialBody: String,

    @ColumnInfo(name = "observation_date")
    val date: String,

    val notes: String,

    @ColumnInfo(name = "scope_setting")
    val scopeSetting: String
)