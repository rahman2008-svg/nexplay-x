package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, CustomPresetEntity::class, AnalyticsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LensoraDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun customPresetDao(): CustomPresetDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: LensoraDatabase? = null

        fun getDatabase(context: Context): LensoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LensoraDatabase::class.java,
                    "lensora_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
