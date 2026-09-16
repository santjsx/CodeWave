package com.codewave.player.core.data

import com.codewave.player.core.media.LrclibLyricsProvider
import com.codewave.player.core.media.LyricsResult
import com.codewave.player.core.model.StreamTrack
import com.codewave.player.core.model.Track
import com.codewave.player.core.network.innertube.InnerTubeClient
import com.codewave.player.core.network.resolver.MetadataResolver
import com.codewave.player.core.network.resolver.ResolvedMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap

interface StreamRepository {
    fun search(query: String): Flow<Result<List<StreamTrack>>>
    fun getExploreCharts(): Flow<Result<List<StreamTrack>>>
    suspend fun resolveStreamTrack(streamTrack: StreamTrack): Result<Track>
    suspend fun resolveSpotifyMetadata(url: String): Result<ResolvedMetadata>
    fun fetchOnlineLyrics(track: Track): Flow<LyricsResult>
}

class DefaultStreamRepository(
    private val innerTubeClient: InnerTubeClient,
    private val metadataResolver: MetadataResolver,
    private val lyricsProvider: LrclibLyricsProvider
) : StreamRepository {

    // In-memory cache for resolved stream URLs to avoid redundant network roundtrips
    private val streamUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()

    override fun search(query: String): Flow<Result<List<StreamTrack>>> = flow {
        emit(innerTubeClient.search(query))
    }.flowOn(Dispatchers.IO)

    override fun getExploreCharts(): Flow<Result<List<StreamTrack>>> = flow {
        emit(innerTubeClient.getExploreCharts())
    }.flowOn(Dispatchers.IO)

    override suspend fun resolveStreamTrack(streamTrack: StreamTrack): Result<Track> {
        val cached = streamUrlCache[streamTrack.id]
        val now = System.currentTimeMillis()

        // Reuse cached stream URL if valid and not within 5 minutes of expiration
        if (cached != null && cached.second > now + (5 * 60 * 1000L)) {
            return Result.success(streamTrack.toTrack(cached.first))
        }

        val result = innerTubeClient.getStreamUrl(streamTrack.id)
        return result.map { streamInfo ->
            streamUrlCache[streamTrack.id] = Pair(streamInfo.streamUrl, streamInfo.expiryEpochMs)
            streamTrack.toTrack(streamInfo.streamUrl)
        }
    }

    override suspend fun resolveSpotifyMetadata(url: String): Result<ResolvedMetadata> {
        return metadataResolver.resolveSpotifyTrack(url)
    }

    override fun fetchOnlineLyrics(track: Track): Flow<LyricsResult> = flow {
        emit(LyricsResult.Loading)
        val result = lyricsProvider.fetchLyrics(track)
        emit(result)
    }.flowOn(Dispatchers.IO)
}
