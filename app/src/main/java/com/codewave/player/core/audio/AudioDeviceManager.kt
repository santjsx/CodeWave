package com.codewave.player.core.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConnectedAudioDevice(
    val id: String,
    val name: String,
    val typeName: String,
    val isWireless: Boolean
)

class AudioDeviceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _currentDevice = MutableStateFlow(determineCurrentOutputDevice())
    val currentDevice: StateFlow<ConnectedAudioDevice> = _currentDevice.asStateFlow()

    private var deviceCallback: AudioDeviceCallback? = null
    var onDeviceChangedListener: ((ConnectedAudioDevice) -> Unit)? = null

    init {
        registerDeviceCallback()
    }

    private fun registerDeviceCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    updateCurrentDevice()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    updateCurrentDevice()
                }
            }
            deviceCallback = callback
            audioManager.registerAudioDeviceCallback(callback, mainHandler)
        }
    }

    fun updateCurrentDevice() {
        val newDevice = determineCurrentOutputDevice()
        val oldDevice = _currentDevice.value
        if (newDevice.id != oldDevice.id) {
            _currentDevice.value = newDevice
            onDeviceChangedListener?.invoke(newDevice)
        }
    }

    private fun determineCurrentOutputDevice(): ConnectedAudioDevice {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            try {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                // Priority order: Bluetooth A2DP/LE > USB Headset > Wired Headphone > Built-in Speaker
                val btDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
                }
                if (btDevice != null) {
                    val rawName = btDevice.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Headset"
                    return ConnectedAudioDevice(
                        id = "bt_${rawName.lowercase().replace(" ", "_")}",
                        name = rawName,
                        typeName = "Bluetooth A2DP",
                        isWireless = true
                    )
                }

                val usbDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
                }
                if (usbDevice != null) {
                    val rawName = usbDevice.productName?.toString()?.takeIf { it.isNotBlank() } ?: "USB DAC"
                    return ConnectedAudioDevice(
                        id = "usb_${rawName.lowercase().replace(" ", "_")}",
                        name = rawName,
                        typeName = "USB DAC / Audio",
                        isWireless = false
                    )
                }

                val wiredDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                }
                if (wiredDevice != null) {
                    return ConnectedAudioDevice(
                        id = "wired_headphones",
                        name = "Wired Headphones",
                        typeName = "3.5mm Analog Output",
                        isWireless = false
                    )
                }

                val speakerDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (speakerDevice != null) {
                    return ConnectedAudioDevice(
                        id = "builtin_speaker",
                        name = "Phone Speaker",
                        typeName = "Internal Speaker",
                        isWireless = false
                    )
                }
            } catch (_: Exception) {}
        }

        return ConnectedAudioDevice(
            id = "default_output",
            name = "Default Audio Output",
            typeName = "System Audio",
            isWireless = false
        )
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null && deviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
            deviceCallback = null
        }
    }
}
