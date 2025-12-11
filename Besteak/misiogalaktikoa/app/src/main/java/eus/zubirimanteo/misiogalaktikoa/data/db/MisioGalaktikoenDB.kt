package eus.zubirimanteo.misiogalaktikoa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import eus.zubirimanteo.misiogalaktikoa.data.dao.AstronautaDAO
import eus.zubirimanteo.misiogalaktikoa.data.dao.MisioDAO
import eus.zubirimanteo.misiogalaktikoa.data.model.Astronauta
import eus.zubirimanteo.misiogalaktikoa.data.model.Misioa
import eus.zubirimanteo.misiogalaktikoa.data.model.Tripulazioa
import kotlinx.coroutines.CoroutineScope

@Database(
    // entitateak
    entities = [
        Astronauta::class,
        Misioa::class,
        Tripulazioa::class
    ],
    // bertsioa
    version = 1,
    // eskemaren babes-kopia
    exportSchema = false
)
abstract class MisioGalaktikoenDB : RoomDatabase() {
    abstract fun astronautaDao(): AstronautaDAO
    abstract fun misioDao(): MisioDAO

    // Singleton
    companion object {  // Javako static hitzaren antzekoa da
        @Volatile
        private var INSTANCE: MisioGalaktikoenDB? = null
        fun getInstance(context: Context, scope : CoroutineScope): MisioGalaktikoenDB {

            val callback = MisioGalaktikoenDBCallback(scope) // DB-a datuz populatzeko

            return INSTANCE ?: synchronized(this) {  // Hari anitzetan segurua izan dadin
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MisioGalaktikoenDB::class.java,
                    "misio_galaktikoen_db"  // Datu-basearen fitxategi-izena
                )
                    .addCallback(callback)  // DB-aren populaziorako
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}

/*
Gemini-ri galdera: Kodea azalduko zenidake? adibidez ez dut ulertzen 39. lerroa zergatik jarri behar den

Gemini:

Funtzio honen helburua MisioGalaktikoenDB-ren instantzia bat itzultzea da.

1.return INSTANCE ?: synchronized(this) { ... }

•Hau Kotlin-en "Elvis operator" (?:) deitzen denaren erabilera oso argia da.
•Lehenik eta behin, INSTANCE aldagaia (gure datu-basearen instantzia gordetzen duena) begiratzen du.
•INSTANCE ez bada null: esan nahi du datu-basea jada sortuta dagoela. Beraz, zuzenean INSTANCE hori hartu eta itzultzen du, eta {...} blokean dagoen kodea ez da exekutatzen.
•INSTANCE null bada: orduan, Elvis operadorearen eskuineko zatia exekutatzen da: synchronized(this) { ... } blokea.

2.synchronized(this) { ... }

•Bloke hau ezinbestekoa da hari (thread) anitzeko inguruneetan, Android aplikazioetan gertatzen den bezala.
•Imajinatu aplikazioaren bi zati desberdin (bi hari) ia aldi berean saiatzen direla datu-basearen instantzia lortzen. Biek egiazta dezakete INSTANCE null dela eta biak saiatuko lirateke instantzia berri bat sortzen. Horrek bi datu-base konexio sortuko lituzke, eta hori da saihestu nahi duguna.
•synchronized blokeak ziurtatzen du une jakin batean hari bakarra sartu daitekeela kode zati horretara. Lehenengo haria sartzen denean, blokeoa jartzen du; bigarrenak itxaron egin behar du lehenengoa amaitu arte. Horrela, instantzia bakarra sortuko dela bermatzen dugu.

3.Blokearen barruko logika:

•val instance = Room.databaseBuilder(...): Hemen gertatzen da magia. Room.databaseBuilder erabiliz, datu-basearen instantzia berria sortzen da. Prozesu astuna izan daiteke, eta horregatik egin nahi dugu behin bakarrik.
•INSTANCE = instance: Sortu berri den instantzia (instance aldagai lokalean gordeta dagoena) gure INSTANCE aldagai estatikoan gordetzen dugu. Horrela, hurrengoan norbaitek getInstance deitzen duenean, INSTANCE jada ez da null izango eta zuzenean itzuliko da.


Eta orain, galderaren muina: Zergatik behar da 39. lerroa?

Erantzun laburra: synchronized blokeak balio bat itzuli behar duelako da, eta Kotlin-en, bloke baten azken espresioa da bloke horren itzulera-balioa.

 */