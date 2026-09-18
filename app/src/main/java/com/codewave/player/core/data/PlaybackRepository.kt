package com.codewave.player.core.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.codewave.player.core.media.CodeWaveMediaSessionService
import com.codewave.player.core.model.ABLoopState
import com.codewave.player.core.model.AudioFormat
import com.codewave.player.core.model.AudioOutputInfo
import com.codewave.player.core.model.DSPStatus
import com.codewave.player.core.model.PlaybackState
import com.codewave.player.core.model.QueueWorkspace
import com.codewave.player.core.model.RepeatMode
import com.codewave.player.core.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface PlaybackRepository {
    val playbackState: StateFlow<PlaybackState>
    fun playTrack(track: Track, queue: List<Track> = listOf(track), startPositionMs: Long = 0L)
    fun playQueue(queue: List<Track>, startIndex: Int = 0, startPositionMs: Long = 0L)
    fun playNext(track: Track)
    fun addToQueue(track: Track)
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun setShuffle(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun setPlaybackSpeed(speed: Float)
    fun toggleFavorite(track: Track)
    val sleepTimerRemainingMs: StateFlow<Long>
    fun startSleepTimer(minutes: Int, finishCurrentTrack: Boolean = false, enableFadeOut: Boolean = true)
    fun stopSleepTimer()
    fun setVolumePercent(percent: Int)
    val audioWaveformBands: StateFlow<FloatArray>

    // Multi-Queue Workspaces
    val queueWorkspaces: StateFlow<List<QueueWorkspace>>
    val viewingWorkspaceId: StateFlow<String>
    fun setViewingWorkspace(workspaceId: String)
    fun createWorkspace(name: String)
    fun deleteWorkspace(workspaceId: String)
    fun clearWorkspace(workspaceId: String)
    fun addTrackToWorkspace(workspaceId: String, track: Track)
    fun removeTrackFromWorkspace(workspaceId: String, trackIndex: Int)
    fun playWorkspace(workspaceId: String, startIndex: Int = 0)

    // A-B Looper (Feature 3.1)
    val abLoopState: StateFlow<ABLoopState>
    fun setLoopPointA(posMs: Long)
    fun setLoopPointB(posMs: Long)
    fun clearABLoop()
    fun toggleABLoop()

    // Dynamic Pitch Shifter (Feature 3.2)
    val pitchSemitones: StateFlow<Int>
    fun setPitchSemitones(semitones: Int)
}

class DefaultPlaybackRepository(
    private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val equalizerRepository: EqualizerRepository? = null,
    private val viperAudioProcessor: com.codewave.player.core.audio.ViperAudioProcessor? = null
) : PlaybackRepository {

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val emptyBands = MutableStateFlow(FloatArray(48))
    override val audioWaveformBands: StateFlow<FloatArray> =
        viperAudioProcessor?.visualizer?.waveformBands ?: emptyBands

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    @Volatile
    private var currentQueue: List<Track> = emptyList()
    private var pendingPlayAction: (() -> Unit)? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var volumeObserver: ContentObserver? = null
    private var volumeReceiver: BroadcastReceiver? = null

    private val _workspaces = MutableStateFlow<List<QueueWorkspace>>(
        listOf(
            QueueWorkspace(id = "main", name = "MAIN", tracks = emptyList(), isPlaybackActive = true),
            QueueWorkspace(id = "scratchpad", name = "SCRATCHPAD", tracks = emptyList(), isPlaybackActive = false)
        )
    )
    override val queueWorkspaces: StateFlow<List<QueueWorkspace>> = _workspaces.asStateFlow()

    private val _viewingWorkspaceId = MutableStateFlow("main")
    override val viewingWorkspaceId: StateFlow<String> = _viewingWorkspaceId.asStateFlow()

    private val _abLoopState = MutableStateFlow(ABLoopState())
    override val abLoopState: StateFlow<ABLoopState> = _abLoopState.asStateFlow()

    private val _pitchSemitones = MutableStateFlow(0)
    override val pitchSemitones: StateFlow<Int> = _pitchSemitones.asStateFlow()

    init {
        initializeController()
        startPositionTracking()
        observeEqualizerStatus()
        startVolumeObservation()
    }

    private fun getSystemVolumePercent(): Int {
        val am = audioManager ?: return 100
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 100
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / max.toFloat()) * 100).roundToInt().coerceIn(0, 100)
    }

    override fun setVolumePercent(percent: Int) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val clampedPercent = percent.coerceIn(0, 100)
        if (max > 0) {
            val target = ((clampedPercent / 100f) * max).roundToInt()
            try {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            } catch (_: Exception) {}
            _playbackState.update { it.copy(volumePercent = clampedPercent) }
        }
    }

    private fun startVolumeObservation() {
        val initialVolume = getSystemVolumePercent()
        _playbackState.update { it.copy(volumePercent = initialVolume) }

        // ContentObserver on Settings.System.CONTENT_URI catches hardware key volume changes
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    val vol = getSystemVolumePercent()
                    _playbackState.update { it.copy(volumePercent = vol) }
                }
            }
            volumeObserver = observer
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {}

        // BroadcastReceiver for standard VOLUME_CHANGED_ACTION
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val vol = getSystemVolumePercent()
                    _playbackState.update { it.copy(volumePercent = vol) }
                }
            }
            volumeReceiver = receiver
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {}
    }

    private fun observeEqualizerStatus() {
        val eqRepo = equalizerRepository ?: return
        scope.launch {
            eqRepo.equalizerConfig.collect { config ->
                val status = if (!config.isEnabled) {
                    DSPStatus.BYPASSED
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    DSPStatus.ACTIVE
                } else {
                    DSPStatus.LIMITED
                }
                _playbackState.update { it.copy(dspStatus = status) }
            }
        }
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
                    val pending = pendingPlayAction
                    if (pending != null) {
                        pendingPlayAction = null
                        pending()
                    } else if (controller?.currentMediaItem == null) {
                        restoreLastPlayedState()
                    }
                } catch (_: Exception) {}
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun restoreLastPlayedState() {
        scope.launch {
            try {
                val lastTrackId = settingsRepository?.lastPlayedTrackId?.firstOrNull() ?: return@launch
                val lastPos = settingsRepository.lastPlayedPositionMs.firstOrNull() ?: 0L
                val track = libraryRepository.getTrackById(lastTrackId) ?: return@launch
                if (_playbackState.value.currentTrack == null) {
                    currentQueue = listOf(track)
                    _playbackState.update {
                        it.copy(
                            currentTrack = track,
                            queue = listOf(track),
                            queueIndex = 0,
                            positionMs = lastPos,
                            durationMs = track.durationMs,
                            isPlaying = false
                        )
                    }

                    withContext(Dispatchers.Main) {
                        val player = controller
                        if (player != null && player.mediaItemCount == 0) {
                            val metadata = MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .setAlbumTitle(track.album)
                                .setArtworkUri(track.albumArtUri?.let { Uri.parse(it) })
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setUri(track.uri)
                                .setMediaId(track.id.toString())
                                .setMediaMetadata(metadata)
                                .build()

                            player.setMediaItem(mediaItem, lastPos.coerceAtLeast(0L))
                            player.prepare()
                            player.pause()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun createPlayerListener() = object : Player.Listener {
        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
            val vol = getSystemVolumePercent()
            _playbackState.update { it.copy(volumePercent = vol) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) {
                viperAudioProcessor?.visualizer?.onPause()
            }
            val current = _playbackState.value.currentTrack
            if (isPlaying && current != null) {
                scope.launch { libraryRepository.recordTrackPlayed(current.id) }
            } else if (!isPlaying && current != null) {
                val pos = controller?.currentPosition ?: _playbackState.value.positionMs
                scope.launch { settingsRepository?.setLastPlayed(current.id, pos.coerceAtLeast(0L)) }
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
            if (mediaItem == null) {
                // Ignore transient null transition during playlist updates/clearing unless player is completely empty and stopped
                if (player.mediaItemCount == 0 && !player.isPlaying) {
                    _playbackState.update { it.copy(currentTrack = null, isPlaying = false) }
                }
                return
            }

            val speed = _playbackState.value.playbackSpeed
            updatePlaybackParameters(speed, _pitchSemitones.value)

            val mediaId = mediaItem.mediaId.toLongOrNull()
            val index = player.currentMediaItemIndex
            val track = (if (mediaId != null) currentQueue.find { it.id == mediaId } else null)
                ?: currentQueue.getOrNull(index)
                ?: _playbackState.value.currentTrack

            if (track?.id != _playbackState.value.currentTrack?.id) {
                clearABLoop()
            }

            val currentPos = player.currentPosition.coerceAtLeast(0L)
            val preservedPos = if (currentPos > 0L) {
                currentPos
            } else if (_playbackState.value.currentTrack?.id == track?.id && _playbackState.value.positionMs > 0L) {
                _playbackState.value.positionMs
            } else {
                0L
            }

            val queueIdx = if (track != null) {
                currentQueue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: index
            } else {
                index
            }

            _playbackState.update {
                it.copy(
                    currentTrack = track,
                    queueIndex = queueIdx,
                    durationMs = player.duration.coerceAtLeast(0L),
                    positionMs = preservedPos,
                    outputInfo = CodeWaveMediaSessionService.getOutputRouteInfo(context)
                )
            }

            if (track != null) {
                if (preservedPos > 0L) {
                    scope.launch { settingsRepository?.setLastPlayed(track.id, preservedPos) }
                } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    scope.launch { settingsRepository?.setLastPlayed(track.id, 0L) }
                }
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
                dspStatus = _playbackState.value.dspStatus
            )
        }
    }

    private fun startPositionTracking() {
        progressJob = scope.launch {
            var lastSaveTimestamp = 0L
            while (isActive) {
                val player = controller
                if (player != null && player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    _playbackState.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }

                    // A-B Looper Boundary Check
                    val loop = _abLoopState.value
                    if (loop.isEnabled && loop.pointA != null && loop.pointB != null && loop.pointB > loop.pointA) {
                        if (pos >= loop.pointB) {
                            seekTo(loop.pointA)
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastSaveTimestamp > 1500L) {
                        lastSaveTimestamp = now
                        val currentTrack = _playbackState.value.currentTrack
                        if (currentTrack != null) {
                            settingsRepository?.setLastPlayed(currentTrack.id, pos)
                        }
                    }
                }
                delay(100) // 10Hz smooth progress polling for scrubber
            }
        }
    }

    override fun playTrack(track: Track, queue: List<Track>, startPositionMs: Long) {
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playQueue(queue, index, startPositionMs)
    }

    @OptIn(UnstableApi::class)
    override fun playQueue(queue: List<Track>, startIndex: Int, startPositionMs: Long) {
        currentQueue = queue
        _workspaces.update { list ->
            list.map { ws ->
                if (ws.isPlaybackActive) ws.copy(tracks = queue) else ws
            }
        }
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

        val track = queue.getOrNull(startIndex) ?: return
        _playbackState.update {
            it.copy(
                queue = queue,
                queueIndex = startIndex,
                currentTrack = track,
                positionMs = startPositionMs.coerceAtLeast(0L),
                isPlaying = true
            )
        }

        val player = controller
        if (player == null) {
            pendingPlayAction = { playQueue(queue, startIndex, startPositionMs) }
            return
        }

        val seekPos = if (startPositionMs > 0L) startPositionMs else C.TIME_UNSET
        player.setMediaItems(mediaItems, startIndex, seekPos)
        if (startPositionMs > 0L) {
            player.seekTo(startIndex, startPositionMs)
        }
        val speed = _playbackState.value.playbackSpeed
        if (speed != 1.0f) {
            player.setPlaybackSpeed(speed)
        }
        player.prepare()
        player.play()

        if (startPositionMs > 0L) {
            scope.launch { settingsRepository?.setLastPlayed(track.id, startPositionMs) }
        }
    }

    private fun createMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.albumArtUri?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    override fun playNext(track: Track) {
        if (currentQueue.isEmpty()) {
            playTrack(track)
            return
        }
        val player = controller
        val currentIndex = player?.currentMediaItemIndex ?: _playbackState.value.queueIndex
        val insertIndex = if (currentIndex >= 0 && currentIndex < currentQueue.size) currentIndex + 1 else currentQueue.size

        val newQueue = currentQueue.toMutableList()
        newQueue.add(insertIndex, track)
        currentQueue = newQueue
        _playbackState.update { it.copy(queue = newQueue) }
        _workspaces.update { list ->
            list.map { ws ->
                if (ws.isPlaybackActive) ws.copy(tracks = newQueue) else ws
            }
        }

        val mediaItem = createMediaItem(track)
        player?.addMediaItem(insertIndex, mediaItem)
    }

    override fun addToQueue(track: Track) {
        if (currentQueue.isEmpty()) {
            playTrack(track)
            return
        }
        val newQueue = currentQueue.toMutableList()
        newQueue.add(track)
        currentQueue = newQueue
        _playbackState.update { it.copy(queue = newQueue) }
        _workspaces.update { list ->
            list.map { ws ->
                if (ws.isPlaybackActive) ws.copy(tracks = newQueue) else ws
            }
        }

        val mediaItem = createMediaItem(track)
        controller?.addMediaItem(mediaItem)
    }

    override fun togglePlayPause() {
        val player = controller
        if (player == null) {
            val current = _playbackState.value.currentTrack
            if (current != null) {
                val q = if (currentQueue.isNotEmpty()) currentQueue else listOf(current)
                playTrack(current, q, _playbackState.value.positionMs)
            }
            return
        }

        if (player.mediaItemCount == 0) {
            val current = _playbackState.value.currentTrack
            if (current != null) {
                val q = if (currentQueue.isNotEmpty()) currentQueue else listOf(current)
                playTrack(current, q, _playbackState.value.positionMs)
                return
            }
        }

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val player = controller
        if (player == null || player.mediaItemCount == 0) {
            val current = _playbackState.value.currentTrack
            if (current != null) {
                _playbackState.update { it.copy(positionMs = positionMs) }
                val q = if (currentQueue.isNotEmpty()) currentQueue else listOf(current)
                playTrack(current, q, positionMs)
                return
            }
        }
        player?.seekTo(positionMs)
        _playbackState.update { it.copy(positionMs = positionMs) }
        val currentTrack = _playbackState.value.currentTrack
        if (currentTrack != null) {
            scope.launch { settingsRepository?.setLastPlayed(currentTrack.id, positionMs) }
        }
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

    private fun updatePlaybackParameters(speed: Float, semitones: Int) {
        val multiplier = 2.0.pow(semitones / 12.0).toFloat()
        controller?.playbackParameters = PlaybackParameters(speed, multiplier)
    }

    override fun setPlaybackSpeed(speed: Float) {
        updatePlaybackParameters(speed, _pitchSemitones.value)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    override fun setPitchSemitones(semitones: Int) {
        val clamped = semitones.coerceIn(-12, 12)
        _pitchSemitones.value = clamped
        updatePlaybackParameters(_playbackState.value.playbackSpeed, clamped)
    }

    override fun setLoopPointA(posMs: Long) {
        val currentDuration = _playbackState.value.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE
        val clampedA = posMs.coerceIn(0L, currentDuration)
        _abLoopState.update { current ->
            val b = current.pointB
            val newB = if (b != null && b <= clampedA) null else b
            current.copy(
                pointA = clampedA,
                pointB = newB,
                isEnabled = newB != null
            )
        }
    }

    override fun setLoopPointB(posMs: Long) {
        val currentDuration = _playbackState.value.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE
        val clampedB = posMs.coerceIn(0L, currentDuration)
        _abLoopState.update { current ->
            val a = current.pointA ?: 0L
            if (clampedB > a) {
                current.copy(
                    pointA = a,
                    pointB = clampedB,
                    isEnabled = true
                )
            } else {
                current
            }
        }
    }

    override fun clearABLoop() {
        _abLoopState.value = ABLoopState()
    }

    override fun toggleABLoop() {
        _abLoopState.update { current ->
            if (current.pointA != null && current.pointB != null && current.pointB > current.pointA) {
                current.copy(isEnabled = !current.isEnabled)
            } else {
                current.copy(isEnabled = false)
            }
        }
    }

    override fun toggleFavorite(track: Track) {
        val newFav = !track.isFavorite
        scope.launch(Dispatchers.IO) {
            libraryRepository.setFavorite(track.id, newFav)
        }
        _playbackState.update { state ->
            val updatedTrack = if (state.currentTrack?.id == track.id) {
                state.currentTrack.copy(isFavorite = newFav)
            } else {
                state.currentTrack
            }
            val updatedQueue = state.queue.map {
                if (it.id == track.id) it.copy(isFavorite = newFav) else it
            }
            currentQueue = updatedQueue
            state.copy(
                currentTrack = updatedTrack,
                queue = updatedQueue
            )
        }
    }

    private val _sleepTimerRemainingMs = MutableStateFlow(0L)
    override val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs.asStateFlow()
    private var sleepTimerJob: Job? = null

    override fun startSleepTimer(minutes: Int, finishCurrentTrack: Boolean, enableFadeOut: Boolean) {
        sleepTimerJob?.cancel()
        controller?.volume = 1.0f
        if (minutes <= 0) {
            _sleepTimerRemainingMs.value = 0L
            return
        }

        val totalMs = minutes * 60 * 1000L
        val targetEndTimeMs = android.os.SystemClock.elapsedRealtime() + totalMs
        _sleepTimerRemainingMs.value = totalMs

        sleepTimerJob = scope.launch {
            while (isActive) {
                val now = android.os.SystemClock.elapsedRealtime()
                val remaining = (targetEndTimeMs - now).coerceAtLeast(0L)
                _sleepTimerRemainingMs.value = remaining

                // Exponential volume fade-out during final 10 seconds if not waiting for track finish
                if (!finishCurrentTrack && enableFadeOut && remaining in 1L..10000L) {
                    val progress = remaining.toFloat() / 10000f
                    val volumeFactor = (progress * progress).coerceIn(0.01f, 1.0f)
                    controller?.volume = volumeFactor
                } else if (!finishCurrentTrack) {
                    controller?.volume = 1.0f
                }

                if (remaining <= 0L) {
                    break
                }
                delay(250L) // 250ms polling ensures accurate second transitions without drift
            }

            if (isActive) {
                // If finishCurrentTrack is enabled, continue until track finishes
                if (finishCurrentTrack && controller?.isPlaying == true) {
                    controller?.volume = 1.0f
                    val initialMediaId = controller?.currentMediaItem?.mediaId
                    while (isActive && controller?.isPlaying == true && controller?.currentMediaItem?.mediaId == initialMediaId) {
                        val pos = controller?.currentPosition ?: 0L
                        val dur = controller?.duration ?: 0L
                        if (dur > 0 && enableFadeOut && (dur - pos) in 1L..10000L) {
                            val fadeRemaining = (dur - pos).coerceAtLeast(0L)
                            val progress = fadeRemaining.toFloat() / 10000f
                            controller?.volume = (progress * progress).coerceIn(0.01f, 1.0f)
                        }
                        delay(250L)
                    }
                }

                controller?.pause()
                controller?.volume = 1.0f
                _sleepTimerRemainingMs.value = 0L
            }
        }
    }

    override fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        controller?.volume = 1.0f
        _sleepTimerRemainingMs.value = 0L
    }

    override fun setViewingWorkspace(workspaceId: String) {
        _viewingWorkspaceId.value = workspaceId
    }

    override fun createWorkspace(name: String) {
        val trimmed = name.trim().ifBlank { "QUEUE ${_workspaces.value.size + 1}" }
        val id = "ws_${System.currentTimeMillis()}"
        _workspaces.update { it + QueueWorkspace(id = id, name = trimmed.uppercase(), tracks = emptyList(), isPlaybackActive = false) }
        _viewingWorkspaceId.value = id
    }

    override fun deleteWorkspace(workspaceId: String) {
        if (workspaceId == "main") return
        _workspaces.update { list -> list.filterNot { it.id == workspaceId } }
        if (_viewingWorkspaceId.value == workspaceId) {
            _viewingWorkspaceId.value = "main"
        }
    }

    override fun clearWorkspace(workspaceId: String) {
        _workspaces.update { list ->
            list.map { ws ->
                if (ws.id == workspaceId) ws.copy(tracks = emptyList()) else ws
            }
        }
        val isPlayback = _workspaces.value.find { it.id == workspaceId }?.isPlaybackActive == true
        if (isPlayback) {
            currentQueue = emptyList()
            _playbackState.update { it.copy(queue = emptyList(), currentTrack = null, isPlaying = false) }
            controller?.clearMediaItems()
        }
    }

    override fun addTrackToWorkspace(workspaceId: String, track: Track) {
        _workspaces.update { list ->
            list.map { ws ->
                if (ws.id == workspaceId) ws.copy(tracks = ws.tracks + track) else ws
            }
        }
        val targetWs = _workspaces.value.find { it.id == workspaceId }
        if (targetWs?.isPlaybackActive == true) {
            addToQueue(track)
        }
    }

    override fun removeTrackFromWorkspace(workspaceId: String, trackIndex: Int) {
        _workspaces.update { list ->
            list.map { ws ->
                if (ws.id == workspaceId) {
                    val updated = ws.tracks.toMutableList()
                    if (trackIndex in updated.indices) {
                        updated.removeAt(trackIndex)
                    }
                    ws.copy(tracks = updated)
                } else ws
            }
        }
    }

    override fun playWorkspace(workspaceId: String, startIndex: Int) {
        val ws = _workspaces.value.find { it.id == workspaceId } ?: return
        if (ws.tracks.isEmpty()) return

        _workspaces.update { list ->
            list.map { it.copy(isPlaybackActive = it.id == workspaceId) }
        }
        _viewingWorkspaceId.value = workspaceId
        playQueue(ws.tracks, startIndex)
    }
}
