package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalPath: String,
    val secureCachedPath: String,
    val fileName: String,
    val mediaType: String, // "PHOTO", "VIDEO", "AUDIO"
    val originalSize: Long,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items")
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val mediaPath: String,
    val mediaTitle: String,
    val mediaArtist: String = "",
    val mediaDuration: Long = 0L,
    val mediaType: String = "AUDIO" // "AUDIO" or "VIDEO"
)

@Entity(tableName = "favorites")
data class FavoriteItem(
    @PrimaryKey val mediaPath: String,
    val mediaType: String, // "PHOTO", "AUDIO", "VIDEO"
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey val mediaPath: String,
    val title: String,
    val artist: String = "",
    val mediaType: String, // "AUDIO", "VIDEO"
    val duration: Long = 0L,
    val lastPosition: Long = 0L,
    val lastPlayedTime: Long = System.currentTimeMillis()
)
