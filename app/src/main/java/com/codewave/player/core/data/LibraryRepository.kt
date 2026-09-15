package com.codewave.player.core.data

import com.codewave.player.core.database.dao.LibraryStats
import com.codewave.player.core.database.dao.PlaylistDao
import com.codewave.player.core.database.dao.TrackDao
import com.codewave.player.core.database.entity.PlaylistEntity
import com.codewave.player.core.database.entity.PlaylistTrackCrossRef
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.Artist
import com.codewave.player.core.model.Playlist
import com.codewave.player.core.model.SearchResult
import com.codewave.player.core.model.Track
import com.codewave.player.core.scanner.AudioScanner
import com.codewave.player.core.scanner.ScanProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface LibraryRepository {
    val scanProgress: StateFlow<ScanProgress>
    suspend fun scanLibrary(): Int
    fun getAllTracks(): Flow<List<Track>>
    fun getAllAlbums(): Flow<List<Album>>
    fun getAllArtists(): Flow<List<Artist>>
    fun getTracksByAlbum(album: String): Flow<List<Track>>
    fun getTracksByArtist(artist: String): Flow<List<Track>>
    fun getFavoriteTracks(): Flow<List<Track>>
    fun getRecentlyAdded(): Flow<List<Track>>
    fun getRecentlyPlayed(): Flow<List<Track>>
    suspend fun getTrackById(trackId: Long): Track?
    fun search(query: String): Flow<SearchResult>
    suspend fun setFavorite(trackId: Long, isFavorite: Boolean)
    suspend fun recordTrackPlayed(trackId: Long)
    fun getLibraryStats(): Flow<LibraryStats>
    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    suspend fun deletePlaylist(playlistId: Long)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
}

class DefaultLibraryRepository(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val audioScanner: AudioScanner
) : LibraryRepository {

    override val scanProgress: StateFlow<ScanProgress> = audioScanner.scanProgress

    override suspend fun scanLibrary(): Int = audioScanner.scanLibrary()

    override suspend fun getTrackById(trackId: Long): Track? {
        return trackDao.getTrackById(trackId)?.toDomain()
    }

    override fun getAllTracks(): Flow<List<Track>> {
        return trackDao.getAllTracksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAlbums(): Flow<List<Album>> {
        return trackDao.getAllAlbumsFlow().map { summaries ->
            summaries.mapIndexed { index, summary ->
                Album(
                    id = index.toLong() + 1,
                    title = summary.album,
                    artist = summary.artist,
                    trackCount = summary.trackCount,
                    year = summary.year,
                    artworkUri = summary.albumArtUri,
                    isHiRes = summary.hasHiRes,
                    isLossless = summary.hasLossless
                )
            }
        }
    }

    override fun getAllArtists(): Flow<List<Artist>> {
        return trackDao.getAllArtistsFlow().map { summaries ->
            summaries.mapIndexed { index, summary ->
                Artist(
                    id = index.toLong() + 1,
                    name = summary.artist,
                    trackCount = summary.trackCount,
                    albumCount = summary.albumCount
                )
            }
        }
    }

    override fun getTracksByAlbum(album: String): Flow<List<Track>> {
        return trackDao.getTracksByAlbumFlow(album).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTracksByArtist(artist: String): Flow<List<Track>> {
        return trackDao.getTracksByArtistFlow(artist).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteTracks(): Flow<List<Track>> {
        return trackDao.getFavoriteTracksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentlyAdded(): Flow<List<Track>> {
        return trackDao.getRecentlyAddedFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentlyPlayed(): Flow<List<Track>> {
        return trackDao.getRecentlyPlayedFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun search(query: String): Flow<SearchResult> = flow {
        if (query.isBlank()) {
            emit(SearchResult(query))
            return@flow
        }

        // FTS sanitized match query (PRD Section 16, 47)
        val sanitized = query.trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }

        val trackEntities = try {
            trackDao.searchTracks(sanitized)
        } catch (_: Exception) {
            emptyList()
        }

        val domainTracks = trackEntities.map { it.toDomain() }

        // Extract matching distinct albums and artists from matches
        val albums = domainTracks
            .groupBy { it.album }
            .entries
            .toList()
            .mapIndexed { idx, entry ->
                val albumName = entry.key
                val tracks = entry.value
                val first = tracks.first()
                Album(
                    id = idx.toLong() + 1,
                    title = albumName,
                    artist = first.artist,
                    trackCount = tracks.size,
                    year = first.year,
                    artworkUri = first.albumArtUri,
                    isHiRes = tracks.any { it.isHiRes },
                    isLossless = tracks.any { it.isLossless }
                )
            }

        val artists = domainTracks
            .groupBy { it.artist }
            .entries
            .toList()
            .mapIndexed { idx, entry ->
                val artistName = entry.key
                val tracks = entry.value
                Artist(
                    id = idx.toLong() + 1,
                    name = artistName,
                    trackCount = tracks.size,
                    albumCount = tracks.map { it.album }.distinct().size
                )
            }

        emit(
            SearchResult(
                query = query,
                tracks = domainTracks,
                albums = albums,
                artists = artists
            )
        )
    }

    override suspend fun setFavorite(trackId: Long, isFavorite: Boolean) {
        trackDao.setFavorite(trackId, isFavorite)
    }

    override suspend fun recordTrackPlayed(trackId: Long) {
        trackDao.recordTrackPlayed(trackId, System.currentTimeMillis())
    }

    override fun getLibraryStats(): Flow<LibraryStats> = trackDao.getLibraryStatsFlow()

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsFlow().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    trackCount = 0,
                    createdAt = entity.createdAt,
                    modifiedAt = entity.modifiedAt,
                    isSmart = entity.isSmart,
                    smartType = entity.smartType
                )
            }
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.clearPlaylistTracks(playlistId)
        playlistDao.deletePlaylistById(playlistId)
    }

    override fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylistFlow(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = System.currentTimeMillis().toInt()
            )
        )
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }
}
