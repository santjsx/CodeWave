package com.codewave.player.core.ota

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.codewave.player.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSizeBytes: Long
) {
    val apkSizeMb: Double get() = if (apkSizeBytes > 0L) apkSizeBytes.toDouble() / (1024.0 * 1024.0) else 4.4
}

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpdateAvailable(val info: UpdateInfo) : UpdateStatus
    data class UpToDate(val version: String) : UpdateStatus
    data class Downloading(val progressPercent: Int, val downloadedMb: Double, val totalMb: Double) : UpdateStatus
    data class ReadyToInstall(val apkFile: File) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class OtaUpdateManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("codewave_ota_prefs", Context.MODE_PRIVATE)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12L, TimeUnit.SECONDS)
        .readTimeout(60L, TimeUnit.SECONDS)
        .writeTimeout(60L, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadMutex = Mutex()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "CodeWave Updates"
            val descriptionText = "Notifications for new CodeWave app releases and updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendUpdateAvailableNotification(info: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted, skipping OTA notification")
                return
            }
        }

        val lastNotifiedVersion = prefs.getString(KEY_LAST_NOTIFIED_VERSION, "")
        if (lastNotifiedVersion == info.latestVersion) {
            Log.d(TAG, "Already notified user for version ${info.latestVersion}, skipping duplicate")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_OTA, true)
            putExtra(EXTRA_LATEST_VERSION, info.latestVersion)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("New CodeWave Update Available (v${info.latestVersion})")
            .setContentText("A new version is ready with improvements. Tap to install now.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A new version of CodeWave (v${info.latestVersion}) is ready.\nTap to open Settings and install seamlessly.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)

        prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, info.latestVersion).apply()
        Log.i(TAG, "Sent push notification for OTA update v${info.latestVersion}")
    }

    val currentAppVersion: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.3.5"
        } catch (_: Exception) {
            "1.3.5"
        }

    suspend fun checkForUpdates(
        owner: String = "santjsx",
        repo: String = "CodeWave",
        showNotificationIfAvailable: Boolean = false
    ): UpdateStatus {
        return withContext(Dispatchers.IO) {
            _updateStatus.value = UpdateStatus.Checking
            try {
                val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "CodeWave-OTA-Updater")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val status = if (response.code == 404) {
                        UpdateStatus.UpToDate(currentAppVersion)
                    } else {
                        UpdateStatus.Error("GitHub API response code: ${response.code}")
                    }
                    _updateStatus.value = status
                    return@withContext status
                }

                val bodyString = response.body?.string() ?: run {
                    val status = UpdateStatus.Error("Empty response from GitHub")
                    _updateStatus.value = status
                    return@withContext status
                }

                val json = JSONObject(bodyString)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseName = json.optString("name", "Version $tagName")
                val releaseNotes = json.optString("body", "No release notes provided.")
                val currentVersion = currentAppVersion.removePrefix("v").trim()

                // Find APK in release assets
                val assets: JSONArray? = json.optJSONArray("assets")
                var apkDownloadUrl = ""
                var apkSize = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset: JSONObject = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                val isNewer = isVersionNewer(tagName, currentVersion)
                val status = if (isNewer && apkDownloadUrl.isNotBlank()) {
                    val info = UpdateInfo(
                        hasUpdate = true,
                        currentVersion = currentAppVersion,
                        latestVersion = tagName,
                        releaseTitle = releaseName,
                        releaseNotes = releaseNotes,
                        downloadUrl = apkDownloadUrl,
                        apkSizeBytes = apkSize
                    )
                    if (showNotificationIfAvailable) {
                        sendUpdateAvailableNotification(info)
                    }
                    UpdateStatus.UpdateAvailable(info)
                } else {
                    UpdateStatus.UpToDate(currentAppVersion)
                }

                _updateStatus.value = status
                status
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for OTA update", e)
                val status = UpdateStatus.Error("Update check failed: ${e.localizedMessage ?: "Network error"}")
                _updateStatus.value = status
                status
            }
        }
    }

    suspend fun downloadAndInstall(info: UpdateInfo) {
        if (!downloadMutex.tryLock()) {
            Log.w(TAG, "Download already in progress, ignoring duplicate request")
            return
        }

        try {
            val totalSizeMb = if (info.apkSizeMb > 0.1) info.apkSizeMb else 4.4

            // Pre-flight disk space check: require 2x APK size or 25MB
            val requiredBytes = if (info.apkSizeBytes > 0L) info.apkSizeBytes * 2 else 25 * 1024 * 1024L
            val availableBytes = try {
                android.os.StatFs(context.cacheDir.path).availableBytes
            } catch (e: Exception) {
                Long.MAX_VALUE
            }

            if (availableBytes < requiredBytes) {
                _updateStatus.value = UpdateStatus.Error("Insufficient storage space to download update. Please free up space.")
                return
            }

            // Immediate UI feedback at 0ms
            _updateStatus.value = UpdateStatus.Downloading(0, 0.0, totalSizeMb)

            withContext(Dispatchers.IO) {
                try {
                    val otaDir = File(context.cacheDir, "ota").apply {
                        mkdirs()
                        listFiles()?.forEach { if (it.name.endsWith(".apk")) it.delete() }
                    }
                    val targetFile = File(otaDir, "CodeWave-v${info.latestVersion}.apk")

                    val request = Request.Builder()
                        .url(info.downloadUrl)
                        .header("User-Agent", "CodeWave-OTA-Updater")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        _updateStatus.value = UpdateStatus.Error("Download failed with HTTP ${response.code}")
                        return@withContext
                    }

                    val responseBody = response.body ?: run {
                        _updateStatus.value = UpdateStatus.Error("Empty download body")
                        return@withContext
                    }

                    val rawContentLength = responseBody.contentLength()
                    val contentLength = if (rawContentLength > 0L) rawContentLength else info.apkSizeBytes
                    val finalTotalMb = if (contentLength > 0L) contentLength.toDouble() / (1024.0 * 1024.0) else totalSizeMb

                    val inputStream: InputStream = responseBody.byteStream()
                    val outputStream = FileOutputStream(targetFile)

                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastEmitTime = 0L
                    var lastEmitPercent = -1

                    while (true) {
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break

                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead.toLong()

                        val percent = if (contentLength > 0L) {
                            ((totalBytesRead * 100L) / contentLength).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        val downloadedMb = totalBytesRead.toDouble() / (1024.0 * 1024.0)

                        val now = System.currentTimeMillis()
                        if (percent != lastEmitPercent && (now - lastEmitTime >= 30L || percent == 100)) {
                            lastEmitTime = now
                            lastEmitPercent = percent
                            _updateStatus.value = UpdateStatus.Downloading(percent, downloadedMb, finalTotalMb)
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    // Validate downloaded APK integrity
                    if (!targetFile.exists() || targetFile.length() < 100_000L) {
                        _updateStatus.value = UpdateStatus.Error("Corrupted or incomplete update package downloaded")
                        targetFile.delete()
                        return@withContext
                    }

                    _updateStatus.value = UpdateStatus.ReadyToInstall(targetFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading OTA update", e)
                    _updateStatus.value = UpdateStatus.Error("Download failed: ${e.localizedMessage ?: "Network error"}")
                }
            }
        } finally {
            downloadMutex.unlock()
        }
    }

    fun dismissUpdate() {
        _updateStatus.value = UpdateStatus.Idle
    }

    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists() || apkFile.length() < 100_000L) {
                _updateStatus.value = UpdateStatus.Error("Invalid APK file. Please check for update again.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            val resolveInfos = context.packageManager.queryIntentActivities(installIntent, 0)
            for (resolveInfo in resolveInfos) {
                context.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering APK installer", e)
            _updateStatus.value = UpdateStatus.Error("Installation trigger failed: ${e.localizedMessage}")
        }
    }

    fun resetToIdle() {
        _updateStatus.value = UpdateStatus.Idle
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    companion object {
        private const val TAG = "OtaUpdateManager"
        const val CHANNEL_ID = "codewave_ota_updates"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_OPEN_OTA = "extra_open_ota"
        const val EXTRA_LATEST_VERSION = "extra_latest_version"
        private const val KEY_LAST_NOTIFIED_VERSION = "key_last_notified_version"
    }
}
