package com.codewave.player.core.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

class AudioBecomingNoisyReceiver(
    private val context: Context,
    private val onNoisyEvent: () -> Unit
) : BroadcastReceiver() {

    private var isRegistered = false

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            onNoisyEvent()
        }
    }

    fun register() {
        if (!isRegistered) {
            context.registerReceiver(
                this,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(this)
            } catch (_: Exception) {}
            isRegistered = false
        }
    }
}
