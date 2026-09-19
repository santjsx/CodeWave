package com.codewave.player.core.data

import com.codewave.player.core.database.dao.LibraryStats
import com.codewave.player.core.database.dao.PlaylistDao
import com.codewave.player.core.database.dao.TrackDao
import com.codewave.player.core.database.entity.PlaylistEntity
import com.codewave.player.core.database.entity.PlaylistTrackCrossRef
import com.codewave.player.core.model.Album
import com.codewave.player.core.model.AlbumGrouping
import com.codewave.player.core.model.Artist
import com.codewave.player.core.model.Playlist
import com.codewave.player.core.model.SearchResult
import com.codewave.player.core.model.Track
import com.codewave.player.core.scanner.AudioScanner
import com.codewave.player.core.scanner.ScanProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

interface LibraryRepository {
    val scanProgress: StateFlow<ScanProgress>
    suspend fun scanLibrary(): Int
    fun getAllTracks(): Flow<List<Track>>
    fun getAllAlbums(): Flow<List<Album>>
    fun getAllArtists(): Flow<List<Artist>>
    fun getTracksByAlbum(album: String): Flow<List<Track>>
    fun getTracksForAlbum(album: Album): Flow<List<Track>>
    fun getTracksByArtist(artist: String): Flow<List<Track>>
    fun getFavoriteTracks(): Flow<List<Track>>
    fun getRecentlyAdded(): Flow<List<Track>>
    fun getRecentlyPlayed(): Flow<List<Track>>
    fun getHeavyRotationTracks(minPlayCount: Int = 5, limit: Int = 50): Flow<List<Track>>
    suspend fun getTrackById(trackId: Long): Track?
    fun search(query: String): Flow<SearchResult>
    suspend fun setFavorite(trackId: Long, isFavorite: Boolean)
    suspend fun recordTrackPlayed(trackId: Long)
    fun getLibraryStats(): Flow<LibraryStats>
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
}

class DefaultLibraryRepository(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val audioScanner: AudioScanner
) : LibraryRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = playlistDao.getAllPlaylists()
            val smartTypes = existing.filter { it.isSmart }.mapNotNull { it.smartType }.toSet()
            if ("HEAVY_ROTATION" !in smartTypes && existing.none { it.name.equals("Heavy Rotation", ignoreCase = true) }) {
                playlistDao.insertPlaylist(
                    PlaylistEntity(name = "Heavy Rotation", isSmart = true, smartType = "HEAVY_ROTATION")
                )
            }
        }
    }

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
        return getAllTracks().map { tracks ->
            AlbumGrouping.groupTracksIntoAlbums(tracks)
        }
    }

    override fun getAllArtists(): Flow<List<Artist>> {
        return trackDao.getAllArtistsFlow().map { summaries ->
            summaries.map { summary ->
                Artist(
                    id = summary.artist.hashCode().toLong(),
                    name = summary.artist,
                    trackCount = summary.trackCount,
                    albumCount = summary.albumCount
                )
            }
        }
    }

    override fun getTracksByAlbum(album: String): Flow<List<Track>> {
        return getAllTracks().map { tracks ->
            AlbumGrouping.getTracksForAlbum(tracks, album)
        }
    }

    override fun getTracksForAlbum(album: Album): Flow<List<Track>> {
        return getAllTracks().map { tracks ->
            AlbumGrouping.getTracksForAlbum(tracks, album)
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

    override fun getHeavyRotationTracks(minPlayCount: Int, limit: Int): Flow<List<Track>> {
        return trackDao.getHeavyRotationTracksFlow(minPlayCount, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun search(query: String): Flow<SearchResult> = flow {
        if (query.isBlank()) {
            emit(SearchResult(query))
            return@flow
        }

        // Robust FTS query sanitization and fallback (AUD-SEARCH-01)
        val tokens = Regex("[\\p{L}\\p{M}\\p{Nd}]+").findAll(query.trim())
            .map { it.value }
            .filter { it.isNotBlank() }
            .toList()

        val trackEntities = if (tokens.isNotEmpty()) {
            val ftsQuery = tokens.joinToString(" ") { "$it*" }
            try {
                val ftsResults = trackDao.searchTracks(ftsQuery)
                if (ftsResults.isNotEmpty()) {
                    ftsResults
                } else {
                    trackDao.searchTracksFallback(query.trim())
                }
            } catch (_: Exception) {
                trackDao.searchTracksFallback(query.trim())
            }
        } else {
            trackDao.searchTracksFallback(query.trim())
        }

        val domainTracks = trackEntities.map { it.toDomain() }

        // Extract matching distinct albums and artists from matches
        val albums = AlbumGrouping.groupTracksIntoAlbums(domainTracks)

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
                    albumCount = tracks.map { it.album }.distinct().size,
                    artworkUri = tracks.firstOrNull { !it.albumArtUri.isNullOrEmpty() }?.albumArtUri
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
    }.flowOn(Dispatchers.IO)

    override suspend fun setFavorite(trackId: Long, isFavorite: Boolean) {
        trackDao.setFavorite(trackId, isFavorite)
    }

    override suspend fun recordTrackPlayed(trackId: Long) {
        trackDao.recordTrackPlayed(trackId, System.currentTimeMillis())
    }

    override fun getLibraryStats(): Flow<LibraryStats> = combine(
        trackDao.getLibraryStatsFlow(),
        getAllAlbums()
    ) { stats, albums ->
        stats.copy(albumCount = albums.size)
    }

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return combine(
            playlistDao.getAllPlaylistsFlow(),
            trackDao.getLibraryStatsFlow(),
            trackDao.getFavoriteTracksFlow(),
            trackDao.getHeavyRotationTracksFlow(),
            playlistDao.getPlaylistTrackCountsFlow()
        ) { entities, stats, favorites, heavyRotation, customCounts ->
            val customCountsMap = customCounts.associate { it.playlistId to it.trackCount }
            entities.map { entity ->
                val count = when {
                    entity.isSmart && (entity.smartType == "FAVORITES" || entity.name.equals("Favorites", ignoreCase = true)) -> {
                        favorites.size
                    }
                    entity.isSmart && (entity.smartType == "RECENT_ADDED" || entity.name.contains("Recent", ignoreCase = true)) -> {
                        minOf(30, stats.trackCount)
                    }
                    entity.isSmart && (entity.smartType == "HI_RES" || entity.smartType == "LOSSLESS" || entity.name.contains("Lossless", ignoreCase = true)) -> {
                        stats.losslessCount + stats.hiResCount
                    }
                    entity.isSmart && (entity.smartType == "HEAVY_ROTATION" || entity.name.contains("Heavy", ignoreCase = true) || entity.name.contains("Rotation", ignoreCase = true)) -> {
                        heavyRotation.size
                    }
                    entity.isSmart -> {
                        minOf(30, stats.trackCount)
                    }
                    else -> {
                        customCountsMap[entity.id] ?: 0
                    }
                }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    trackCount = count,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getPlaylistByIdFlow(playlistId).flatMapLatest { entity ->
            if (entity == null) {
                flowOf(emptyList())
            } else if (entity.isSmart) {
                when {
                    entity.smartType == "FAVORITES" || entity.name.equals("Favorites", ignoreCase = true) -> {
                        trackDao.getFavoriteTracksFlow().map { list -> list.map { it.toDomain() } }
                    }
                    entity.smartType == "RECENT_ADDED" || entity.name.contains("Recent", ignoreCase = true) -> {
                        trackDao.getRecentlyAddedFlow(limit = 30).map { list -> list.map { it.toDomain() } }
                    }
                    entity.smartType == "HI_RES" || entity.smartType == "LOSSLESS" || entity.name.contains("Lossless", ignoreCase = true) -> {
                        trackDao.getLosslessTracksFlow().map { list -> list.map { it.toDomain() } }
                    }
                    entity.smartType == "HEAVY_ROTATION" || entity.name.contains("Heavy", ignoreCase = true) || entity.name.contains("Rotation", ignoreCase = true) -> {
                        trackDao.getHeavyRotationTracksFlow().map { list -> list.map { it.toDomain() } }
                    }
                    else -> {
                        trackDao.getRecentlyAddedFlow(limit = 30).map { list -> list.map { it.toDomain() } }
                    }
                }
            } else {
                playlistDao.getTracksForPlaylistFlow(playlistId).map { list -> list.map { it.toDomain() } }
            }
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
