package eus.zubirimanteo.astrolog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * AstroLog aplikazioaren Room Datu-basea.
 * [entities] eremuan, erabiliko diren Entity klase guztiak definitu behar dira.
 * [version] datu-basearen bertsioa da. Aldatu Entitatea aldatzen denean (eta migrazioak egin).
 */
@Database(entities = [Observation::class], version = 1, exportSchema = false)
abstract class AstroDatabase : RoomDatabase() {

    // Datu-baseak eskaintzen duen DAO-a (Room-ek inplementatuko du)
    abstract fun observationDao(): ObservationDao

    // Singleton patroia Instantzia bakarra ziurtatzeko.
    companion object {
        @Volatile
        private var INSTANCE: AstroDatabase? = null

        fun getDatabase(context: Context): AstroDatabase {
            // Instantzia dagoeneko existitzen bada, itzuli hori
            return INSTANCE ?: synchronized(this) {
                // Instantzia bikoitza saihesteko blokeoaren barruan berriro egiaztatu
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AstroDatabase::class.java,
                    "astrolog_database" // Datu-base fitxategiaren izena
                )
                    // Honek ez du migraziorik behar bertsioa igotzean (Produkzioan ez da gomendagarria)
                    // .fallbackToDestructiveMigration()
                    // DB importatu. Fitxategia 'assets/databases/astrolog_database.db'
                    //.createFromAsset("assets/databases/astrolog_database.db")
                    .build()
                INSTANCE = instance
                // Eta instantzia itzuli
                instance
            }
        }
    }
}