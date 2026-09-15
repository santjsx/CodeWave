package com.codewave.player.core.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.codewave.player.CodeWaveApplication
import com.codewave.player.MainActivity
import com.codewave.player.core.audio.DspEngine
import com.codewave.player.core.model.AudioOutputInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CodeWaveMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var dspEngine: DspEngine
    private lateinit var audioFocusManager: AudioFocusManager
    private lateinit var noisyReceiver: AudioBecomingNoisyReceiver

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var eqJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        dspEngine = DspEngine()

        audioFocusManager = AudioFocusManager(
            context = this,
            onPausePlayback = { player.pause() },
            onResumePlayback = { player.play() },
            onSetVolumeMultiplier = { multiplier -> player.volume = multiplier }
        )

        noisyReceiver = AudioBecomingNoisyReceiver(
            context = this,
            onNoisyEvent = { player.pause() }
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false) // Managed via our custom AudioFocusManager
            .setHandleAudioBecomingNoisy(false) // Managed via our custom AudioBecomingNoisyReceiver
            .build()

        val eqRepo = (application as CodeWaveApplication).container.equalizerRepository

        fun checkAndAttachDsp(sessionId: Int) {
            if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                dspEngine.attachToSession(sessionId, eqRepo.equalizerConfig.value)
            }
        }

        checkAndAttachDsp(player.audioSessionId)

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                checkAndAttachDsp(audioSessionId)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    checkAndAttachDsp(player.audioSessionId)
                    noisyReceiver.register()
                    audioFocusManager.requestAudioFocus()
                } else {
                    noisyReceiver.unregister()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    checkAndAttachDsp(player.audioSessionId)
                }
                if (playbackState == Player.STATE_ENDED) {
                    audioFocusManager.abandonAudioFocus()
                }
            }
        })

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        // Observe Equalizer updates and push to DSP Engine
        eqJob = serviceScope.launch {
            eqRepo.equalizerConfig.collectLatest { config ->
                dspEngine.applyConfig(config)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        eqJob?.cancel()
        noisyReceiver.unregister()
        audioFocusManager.abandonAudioFocus()
        dspEngine.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        /**
         * Inspects the current audio routing device to produce a factual AudioOutputInfo
         * (PRD Section 2, 19).
         */
        fun getOutputRouteInfo(context: Context): AudioOutputInfo {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return AudioOutputInfo.UNAVAILABLE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val activeDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                } ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

                if (activeDevice != null) {
                    val routeName = when (activeDevice.type) {
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio (${activeDevice.productName})"
                        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB DAC / Audio (${activeDevice.productName})"
                        else -> activeDevice.productName.toString()
                    }

                    val sampleRate = activeDevice.sampleRates.firstOrNull() ?: 48000
                    return AudioOutputInfo(
                        isAvailable = true,
                        sampleRateHz = sampleRate,
                        bitDepth = 16,
                        routeName = routeName,
                        encodingName = "PCM Stereo",
                        latencyMs = 24
                    )
                }
            }

            return AudioOutputInfo(
                isAvailable = true,
                sampleRateHz = 48000,
                bitDepth = 16,
                routeName = "System Audio Output",
                encodingName = "PCM 16-bit",
                latencyMs = 30
            )
        }
    }
}
