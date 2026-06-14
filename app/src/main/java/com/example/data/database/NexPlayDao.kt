package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NexPlayDao {
    
    // VAULT
    @Query("SELECT * FROM vault_items ORDER BY dateAdded DESC")
    fun getAllVaultItems(): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItem)

    @Delete
    suspend fun deleteVaultItem(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE secureCachedPath = :securePath")
    suspend fun deleteVaultItemBySecurePath(securePath: String)

    // PLAYLISTS
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Int)

    // PLAYLIST ITEMS
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY id DESC")
    fun getPlaylistItems(playlistId: Int): Flow<List<PlaylistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaPath = :mediaPath")
    suspend fun removePlaylistItem(playlistId: Int, mediaPath: String)

    // FAVORITES
    @Query("SELECT * FROM favorites ORDER BY dateAdded DESC")
    fun getAllFavorites(): Flow<List<FavoriteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(item: FavoriteItem)

    @Query("DELETE FROM favorites WHERE mediaPath = :mediaPath")
    suspend fun deleteFavoriteByPath(mediaPath: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaPath = :mediaPath LIMIT 1)")
    fun isFavorite(mediaPath: String): Flow<Boolean>

    // HISTORY
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTime DESC")
    fun getPlaybackHistory(): Flow<List<PlaybackHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackHistory(history: PlaybackHistory)

    @Query("DELETE FROM playback_history WHERE mediaPath = :mediaPath")
    suspend fun deleteHistoryByPath(mediaPath: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearAllHistory()
}
