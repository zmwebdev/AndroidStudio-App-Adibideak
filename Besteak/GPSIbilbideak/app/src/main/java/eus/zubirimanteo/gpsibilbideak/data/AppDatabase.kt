package eus.zubirimanteo.gpsibilbideak.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import eus.zubirimanteo.gpsibilbideak.data.dao.GpsPointDao
import eus.zubirimanteo.gpsibilbideak.data.dao.RouteDao
import eus.zubirimanteo.gpsibilbideak.data.dao.RouteWithPointsDao
import eus.zubirimanteo.gpsibilbideak.data.entity.GpsPoint
import eus.zubirimanteo.gpsibilbideak.data.entity.Route

@Database(
    entities = [Route::class, GpsPoint::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun gpsPointDao(): GpsPointDao
    abstract fun routeWithPointsDao(): RouteWithPointsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gps_routes.db"
                )
                    .createFromAsset("database/gps_routes.db")
                    .build().also { INSTANCE = it }
            }
    }
}