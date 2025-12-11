package eus.zubirimanteo.misiogalaktikoa.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

// MisioAstronauta.kt - (Kontsulta egiteko soilik erabiliko den datu-klasea)

data class MisioAstronauta(
    @Embedded val misioa: Misioa, // Gemini: @Embedded erabiliz, esan diezaiokezu Room-i: "Begira, Astronauta klase honek Misioa objektu bat du barruan. Trata ezazu Misioa objektuaren propietate guztiak (id, izena, helmuga) Astronauta taularen zutabeak balira bezala".
    
    @Relation(
        parentColumn = "misioaId", // Misioa (guraso) taula zutabearen izena
        entityColumn = "astronautaId", // Astronauta (semea) taulako zutabea
        associateBy = Junction(Tripulazioa::class)
    )
    val astronautak: List<Astronauta>
)