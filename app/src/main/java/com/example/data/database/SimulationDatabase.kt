package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.models.SavedRoute
import com.example.data.models.RouteHistory
import com.example.data.models.UserAccount
import com.example.data.models.WaypointListConverter

@Database(
    entities = [SavedRoute::class, RouteHistory::class, UserAccount::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(WaypointListConverter::class)
abstract class SimulationDatabase : RoomDatabase() {
    abstract fun dao(): SimulationDao

    companion object {
        @Volatile
        private var INSTANCE: SimulationDatabase? = null

        fun getInstance(context: Context): SimulationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SimulationDatabase::class.java,
                    "world_route_sim_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
