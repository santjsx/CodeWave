package com.codewave.player.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.codewave.player.core.database.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllDownloadsFlow(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status IN ('PENDING', 'CONNECTING', 'DOWNLOADING', 'TAGGING') ORDER BY createdAt ASC")
    fun getActiveDownloadsFlow(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloadsFlow(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(task: DownloadTaskEntity)

    @Update
    suspend fun updateDownload(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String? = null)

    @Query("UPDATE download_tasks SET progress = :progress, downloadedBytes = :downloaded, speedBytesPerSec = :speed WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloaded: Long, speed: Long)

    @Query("UPDATE download_tasks SET status = 'COMPLETED', progress = 1.0, completedAt = :completedAt, localUri = :localUri WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long, localUri: String)

    @Delete
    suspend fun deleteDownload(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun clearCompletedDownloads()
}
