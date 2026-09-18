package com.codewave.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.codewave.player.core.designsystem.theme.CodeWaveTheme
import com.codewave.player.ui.CodeWaveApp

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (audioGranted) {
            triggerBackgroundScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (hasAudioPermission()) {
            triggerBackgroundScan()
        } else {
            checkAndRequestPermissions()
        }

        triggerBackgroundUpdateCheck()

        setContent {
            val app = application as CodeWaveApplication
            val themeId by app.container.settingsRepository.themeId.collectAsState(initial = "obsidian")
            CodeWaveTheme(themeId = themeId) {
                CodeWaveApp(application = app)
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun triggerBackgroundScan() {
        val app = application as CodeWaveApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.container.libraryRepository.scanLibrary()
        }
    }

    private fun triggerBackgroundUpdateCheck() {
        val app = application as CodeWaveApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.otaUpdateManager.checkForUpdates(showNotificationIfAvailable = true)
            } catch (_: Exception) {
                // Fails silently if offline
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
