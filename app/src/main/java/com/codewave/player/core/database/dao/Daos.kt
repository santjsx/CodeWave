package com.codewave.player.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.codewave.player.core.database.entity.EQPresetEntity
import com.codewave.player.core.database.entity.PlaybackHistoryEntity
import com.codewave.player.core.database.entity.PlaylistEntity
import com.codewave.player.core.database.entity.PlaylistTrackCrossRef
import com.codewave.player.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

data class AlbumSummary(
    val album: String,
    val artist: String,
    val trackCount: Int,
    val year: Int?,
    val albumArtUri: String?,
    val hasHiRes: Boolean,
    val hasLossless: Boolean
)

data class ArtistSummary(
    val artist: String,
    val trackCount: Int,
    val albumCount: Int
)

data class LibraryStats(
    val trackCount: Int,
    val albumCount: Int,
    val artistCount: Int,
    val losslessCount: Int,
    val hiResCount: Int
)

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getTrackByMediaStoreId(mediaStoreId: Long): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: Long, isFavorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :trackId")
    suspend fun recordTrackPlayed(trackId: Long, timestamp: Long)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteTracksByMediaStoreIds(mediaStoreIds: List<Long>)

    @Query("""
        SELECT album, artist, COUNT(*) as trackCount, MAX(year) as year, 
               MAX(albumArtUri) as albumArtUri, 
               MAX(isHiRes) as hasHiRes, MAX(isLossless) as hasLossless
        FROM tracks 
        GROUP BY album 
        ORDER BY album COLLATE NOCASE ASC
    """)
    fun getAllAlbumsFlow(): Flow<List<AlbumSummary>>

    @Query("SELECT * FROM tracks WHERE album = :album ORDER BY trackNumber ASC, title ASC")
    fun getTracksByAlbumFlow(album: String): Flow<List<TrackEntity>>

    @Query("""
        SELECT artist, COUNT(*) as trackCount, COUNT(DISTINCT album) as albumCount 
        FROM tracks 
        GROUP BY artist 
        ORDER BY artist COLLATE NOCASE ASC
    """)
    fun getAllArtistsFlow(): Flow<List<ArtistSummary>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY album ASC, trackNumber ASC, title ASC")
    fun getTracksByArtistFlow(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY dateModified DESC")
    fun getFavoriteTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedFlow(limit: Int = 30): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp IS NOT NULL ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentlyPlayedFlow(limit: Int = 30): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        JOIN tracks_fts fts ON t.id = fts.rowid
        WHERE tracks_fts MATCH :query
        ORDER BY t.title COLLATE NOCASE ASC
    """)
    suspend fun searchTracks(query: String): List<TrackEntity>

    @Query("""
        SELECT 
            COUNT(*) as trackCount,
            COUNT(DISTINCT album) as albumCount,
            COUNT(DISTINCT artist) as artistCount,
            SUM(CASE WHEN isLossless = 1 THEN 1 ELSE 0 END) as losslessCount,
            SUM(CASE WHEN isHiRes = 1 THEN 1 ELSE 0 END) as hiResCount
        FROM tracks
    """)
    fun getLibraryStatsFlow(): Flow<LibraryStats>
}

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY modifiedAt DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :newName, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun renamePlaylist(id: Long, newName: String, modifiedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
    """)
    fun getTracksForPlaylistFlow(playlistId: Long): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCountFlow(playlistId: Long): Flow<Int>
}

@Dao
interface EQPresetDao {
    @Query("SELECT * FROM eq_presets ORDER BY isBuiltIn DESC, name ASC")
    fun getAllPresetsFlow(): Flow<List<EQPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EQPresetEntity): Long

    @Delete
    suspend fun deletePreset(preset: EQPresetEntity)
}
