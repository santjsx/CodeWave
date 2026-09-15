package com.codewave.player.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.Track

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["isFavorite"]),
        Index(value = ["dateAdded"])
    ]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long,
    val uri: String,
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val durationMs: Long,
    val fileSize: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val format: String,
    val codec: String,
    val sampleRate: Int,
    val bitDepth: Int? = null,
    val channels: Int = 2,
    val bitrateKbps: Int = 0,
    val isLossless: Boolean = false,
    val isHiRes: Boolean = false,
    val albumArtUri: String? = null,
    val fingerprint: String = "",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long? = null
) {
    fun toDomain(): Track = Track(
        id = id,
        mediaStoreId = mediaStoreId,
        uri = uri,
        path = path,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        discNumber = discNumber,
        durationMs = durationMs,
        fileSize = fileSize,
        dateAdded = dateAdded,
        dateModified = dateModified,
        mimeType = mimeType,
        format = try { AudioFormat.valueOf(format) } catch (_: Exception) { AudioFormat.UNKNOWN },
        codec = codec,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        channels = channels,
        bitrateKbps = bitrateKbps,
        isLossless = isLossless,
        isHiRes = isHiRes,
        albumArtUri = albumArtUri,
        fingerprint = fingerprint,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedTimestamp = lastPlayedTimestamp
    )
}

@Entity(tableName = "tracks_fts")
@Fts4(contentEntity = TrackEntity::class)
data class TrackFtsEntity(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String?
)

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isSmart: Boolean = false,
    val smartType: String? = null
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index(value = ["playlistId"]), Index(value = ["trackId"])]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedTimestamp: Long = System.currentTimeMillis(),
    val completionPercent: Float = 1.0f
)

@Entity(tableName = "eq_presets")
data class EQPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean,
    val preampGainDb: Float,
    val bandGainsJson: String
)
