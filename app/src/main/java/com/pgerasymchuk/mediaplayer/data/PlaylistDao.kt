package com.pgerasymchuk.mediaplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pgerasymchuk.mediaplayer.data.entity.MediaFileEntity
import com.pgerasymchuk.mediaplayer.data.entity.PlaylistEntity

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM media_files WHERE playlist_id = :playlistId")
    suspend fun getMediaFilesForPlaylist(playlistId: Long): List<MediaFileEntity>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    suspend fun insertMediaFile(mediaFile: MediaFileEntity) : Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM media_files WHERE playlist_id = :playlistId AND id = :mediaId")
    suspend fun deleteMediaFile(playlistId: Long, mediaId: Long)
}