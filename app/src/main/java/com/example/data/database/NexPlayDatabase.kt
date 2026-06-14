package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VaultItem::class,
        Playlist::class,
        PlaylistItem::class,
        FavoriteItem::class,
        PlaybackHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NexPlayDatabase : RoomDatabase() {
    abstract fun nexPlayDao(): NexPlayDao

    companion object {
        @Volatile
        private var INSTANCE: NexPlayDatabase? = null

        fun getDatabase(context: Context): NexPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexPlayDatabase::class.java,
                    "nexplay_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
