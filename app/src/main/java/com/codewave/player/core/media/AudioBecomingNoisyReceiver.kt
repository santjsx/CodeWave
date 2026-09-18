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
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        this,
                        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    context.registerReceiver(
                        this,
                        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    )
                }
                isRegistered = true
            } catch (_: Exception) {}
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
