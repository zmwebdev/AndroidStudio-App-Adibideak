package eus.zubirimanteo.misiogalaktikoa.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MisioGalaktikoenDBCallback(
    private val scope: CoroutineScope,  // Zergatik behar du klase honek CoroutineScope bat? DB-arekin lan egiten duelako
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        scope.launch {
            // SQL erabiltzen da
            db.execSQL("INSERT INTO astronautak (izena, espezialitatea) VALUES ('Neil Armstrong', 'Komandantea')")
            db.execSQL("INSERT INTO astronautak (izena, espezialitatea) VALUES ('Yuri Gagarin', 'Pilotoa')")
            db.execSQL("INSERT INTO misioak (izena, hasieraData) VALUES ('Apollo 11', 1000000000)")
        }
    }
}