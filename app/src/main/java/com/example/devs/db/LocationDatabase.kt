package com.example.devs.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.devs.db.dao.LocationDao
import com.example.devs.db.entity.LocationEntity

@Database(entities = [LocationEntity::class], version = 1)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    companion object {
        @Volatile
        private var INSTANCE: LocationDatabase? = null

        fun getDatabase(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDatabase::class.java,
                    "location_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
