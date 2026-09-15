package com.codewave.player.core.data

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.codewave.player.core.media.CodeWaveMediaSessionService
import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.AudioOutputInfo
import com.codewave.player.core.model.DSPStatus
import com.codewave.player.core.model.PlaybackState
import com.codewave.player.core.model.RepeatMode
import com.codewave.player.core.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface PlaybackRepository {
    val playbackState: StateFlow<PlaybackState>
    fun playTrack(track: Track, queue: List<Track> = listOf(track))
    fun playQueue(queue: List<Track>, startIndex: Int = 0)
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun setShuffle(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun setPlaybackSpeed(speed: Float)
}

class DefaultPlaybackRepository(
    private val context: Context,
    private val libraryRepository: LibraryRepository
) : PlaybackRepository {

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var currentQueue: List<Track> = emptyList()

    init {
        initializeController()
        startPositionTracking()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, CodeWaveMediaSessionService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    controller = controllerFuture?.get()?.apply {
                        addListener(createPlayerListener())
                        updateStateFromPlayer(this)
                    }
                } catch (_: Exception) {}
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun createPlayerListener() = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            val current = _playbackState.value.currentTrack
            if (isPlaying && current != null) {
                scope.launch { libraryRepository.recordTrackPlayed(current.id) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = controller?.duration?.takeIf { d -> d > 0 } ?: it.durationMs
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = controller ?: return
            val index = player.currentMediaItemIndex
            val track = currentQueue.getOrNull(index)

            _playbackState.update {
                it.copy(
                    currentTrack = track,
                    queueIndex = index,
                    durationMs = player.duration.coerceAtLeast(0L),
                    positionMs = 0L,
                    outputInfo = CodeWaveMediaSessionService.getOutputRouteInfo(context)
                )
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mode = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            _playbackState.update { it.copy(repeatMode = mode) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(shuffleMode = shuffleModeEnabled) }
        }
    }

    private fun updateStateFromPlayer(player: Player) {
        val index = player.currentMediaItemIndex
        val track = currentQueue.getOrNull(index)
        _playbackState.update {
            it.copy(
                currentTrack = track,
                queueIndex = index,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                shuffleMode = player.shuffleModeEnabled,
                repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                },
                outputInfo = CodeWaveMediaSessionService.getOutputRouteInfo(context),
                dspStatus = DSPStatus.ACTIVE
            )
        }
    }

    private fun startPositionTracking() {
        progressJob = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null && player.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(250) // Smooth 4Hz progress polling for scrubber
            }
        }
    }

    override fun playTrack(track: Track, queue: List<Track>) {
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playQueue(queue, index)
    }

    @OptIn(UnstableApi::class)
    override fun playQueue(queue: List<Track>, startIndex: Int) {
        currentQueue = queue
        val mediaItems = queue.map { track ->
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.albumArtUri?.let { Uri.parse(it) })
                .build()

            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(metadata)
                .build()
        }

        val player = controller ?: return
        player.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        player.prepare()
        player.play()

        _playbackState.update {
            it.copy(
                queue = queue,
                queueIndex = startIndex,
                currentTrack = queue.getOrNull(startIndex),
                isPlaying = true
            )
        }
    }

    override fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _playbackState.update { it.copy(positionMs = positionMs) }
    }

    override fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    override fun skipPrevious() {
        controller?.let { player ->
            if (player.currentPosition > 3000L) {
                player.seekTo(0L)
            } else {
                player.seekToPreviousMediaItem()
            }
        }
    }

    override fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        _playbackState.update { it.copy(shuffleMode = enabled) }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        val exMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        controller?.repeatMode = exMode
        _playbackState.update { it.copy(repeatMode = mode) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }
}
