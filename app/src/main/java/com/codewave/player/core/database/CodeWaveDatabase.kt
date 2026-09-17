package com.codewave.player.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codewave.player.core.database.dao.EQPresetDao
import com.codewave.player.core.database.dao.PlaylistDao
import com.codewave.player.core.database.dao.TrackDao
import com.codewave.player.core.database.entity.EQPresetEntity
import com.codewave.player.core.database.entity.PlaybackHistoryEntity
import com.codewave.player.core.database.entity.PlaylistEntity
import com.codewave.player.core.database.entity.PlaylistTrackCrossRef
import com.codewave.player.core.database.entity.TrackEntity
import com.codewave.player.core.database.entity.TrackFtsEntity
import com.codewave.player.core.model.EQPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrackEntity::class,
        TrackFtsEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        EQPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CodeWaveDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun eqPresetDao(): EQPresetDao

    companion object {
        @Volatile
        private var INSTANCE: CodeWaveDatabase? = null

        fun getInstance(context: Context): CodeWaveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeWaveDatabase::class.java,
                    "codewave_library.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getInstance(context))
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                ensurePresetsSeeded(getInstance(context))
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: CodeWaveDatabase) {
            // Prepopulate initial standard playlists
            val playlistDao = db.playlistDao()
            playlistDao.insertPlaylist(
                PlaylistEntity(name = "Favorites", isSmart = true, smartType = "FAVORITES")
            )
            playlistDao.insertPlaylist(
                PlaylistEntity(name = "Recently Added", isSmart = true, smartType = "RECENT_ADDED")
            )
            playlistDao.insertPlaylist(
                PlaylistEntity(name = "Lossless & Hi-Res", isSmart = true, smartType = "HI_RES")
            )

            ensurePresetsSeeded(db)
        }

        suspend fun ensurePresetsSeeded(db: CodeWaveDatabase) {
            val eqPresetDao = db.eqPresetDao()
            val builtInCount = eqPresetDao.getBuiltInPresetCount()
            if (builtInCount < EQPreset.PRESETS_10_BAND.size) {
                val existing = eqPresetDao.getAllPresets()
                val existingNames = existing.filter { it.isBuiltIn }.map { it.name.lowercase() }.toSet()
                val missingEntities = EQPreset.PRESETS_10_BAND
                    .filter { it.name.lowercase() !in existingNames }
                    .map { preset ->
                        EQPresetEntity(
                            name = preset.name,
                            isBuiltIn = preset.isBuiltIn,
                            preampGainDb = preset.preampGainDb,
                            bandGainsJson = preset.bandGainsDb.joinToString(",")
                        )
                    }
                if (missingEntities.isNotEmpty()) {
                    eqPresetDao.insertPresets(missingEntities)
                }
            }
        }
    }
}
