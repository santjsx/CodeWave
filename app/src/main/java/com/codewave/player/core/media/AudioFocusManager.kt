package com.codewave.player.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AudioFocusManager(
    context: Context,
    private val onPausePlayback: () -> Unit,
    private val onResumePlayback: () -> Unit,
    private val onSetVolumeMultiplier: (Float) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    var resumeOnFocusGain = false
        internal set

    var isTransientPause = false
        internal set

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                onPausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = true
                isTransientPause = true
                try {
                    onPausePlayback()
                } finally {
                    isTransientPause = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume cleanly during navigation / notifications (PRD Section 30)
                onSetVolumeMultiplier(0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                onSetVolumeMultiplier(1.0f)
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    onResumePlayback()
                }
            }
        }
    }

    fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            focusRequest = request
            when (audioManager.requestAudioFocus(request)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    resumeOnFocusGain = false
                    true
                }
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    // Arm delayed resumption so AUDIOFOCUS_GAIN will trigger playback once clear
                    resumeOnFocusGain = true
                    false
                }
                else -> {
                    resumeOnFocusGain = false
                    false
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val granted = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            resumeOnFocusGain = false
            granted
        }
    }

    fun onUserPaused() {
        resumeOnFocusGain = false
    }

    fun abandonAudioFocus() {
        resumeOnFocusGain = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }
}
