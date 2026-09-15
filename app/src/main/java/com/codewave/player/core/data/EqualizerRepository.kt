package com.codewave.player.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codewave.player.core.database.dao.EQPresetDao
import com.codewave.player.core.database.entity.EQPresetEntity
import com.codewave.player.core.model.EQPreset
import com.codewave.player.core.model.EqualizerBand
import com.codewave.player.core.model.EqualizerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.eqDataStore by preferencesDataStore(name = "codewave_eq_prefs")

interface EqualizerRepository {
    val equalizerConfig: Flow<EqualizerConfig>
    val presets: Flow<List<EQPreset>>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setPreampGain(gainDb: Float)
    suspend fun setLimiterEnabled(enabled: Boolean)
    suspend fun setBandGain(bandIndex: Int, gainDb: Float)
    suspend fun applyPreset(preset: EQPreset)
    suspend fun saveCustomPreset(name: String, preampDb: Float, gains: List<Float>)
}

class DefaultEqualizerRepository(
    private val context: Context,
    private val eqPresetDao: EQPresetDao
) : EqualizerRepository {

    private object PreferencesKeys {
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val PREAMP_GAIN = floatPreferencesKey("preamp_gain")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val ACTIVE_PRESET = stringPreferencesKey("active_preset")
        val BAND_GAINS = stringPreferencesKey("band_gains")
    }

    override val equalizerConfig: Flow<EqualizerConfig> = context.eqDataStore.data.map { prefs ->
        val enabled = prefs[PreferencesKeys.EQ_ENABLED] ?: false
        val preamp = prefs[PreferencesKeys.PREAMP_GAIN] ?: 0f
        val limiter = prefs[PreferencesKeys.LIMITER_ENABLED] ?: true
        val preset = prefs[PreferencesKeys.ACTIVE_PRESET] ?: "Flat"
        val gainsStr = prefs[PreferencesKeys.BAND_GAINS] ?: "0,0,0,0,0,0,0,0,0,0"

        val gains = gainsStr.split(",").mapNotNull { it.toFloatOrNull() }
        val defaultBands = EqualizerConfig.defaultBands()
        val bands = defaultBands.mapIndexed { index, band ->
            band.copy(gainDb = gains.getOrElse(index) { 0f })
        }

        EqualizerConfig(
            isEnabled = enabled,
            preampGainDb = preamp,
            isLimiterEnabled = limiter,
            activePresetName = preset,
            bands = bands
        )
    }

    override val presets: Flow<List<EQPreset>> = eqPresetDao.getAllPresetsFlow().map { entities ->
        entities.map { entity ->
            val gains = entity.bandGainsJson.split(",").mapNotNull { it.toFloatOrNull() }
            EQPreset(
                id = entity.id,
                name = entity.name,
                isBuiltIn = entity.isBuiltIn,
                preampGainDb = entity.preampGainDb,
                bandGainsDb = gains
            )
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[PreferencesKeys.EQ_ENABLED] = enabled }
    }

    override suspend fun setPreampGain(gainDb: Float) {
        context.eqDataStore.edit { it[PreferencesKeys.PREAMP_GAIN] = gainDb }
    }

    override suspend fun setLimiterEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[PreferencesKeys.LIMITER_ENABLED] = enabled }
    }

    override suspend fun setBandGain(bandIndex: Int, gainDb: Float) {
        context.eqDataStore.edit { prefs ->
            val gainsStr = prefs[PreferencesKeys.BAND_GAINS] ?: "0,0,0,0,0,0,0,0,0,0"
            val gains = gainsStr.split(",").mapNotNull { it.toFloatOrNull() }.toMutableList()
            while (gains.size < 10) gains.add(0f)
            if (bandIndex in gains.indices) {
                gains[bandIndex] = gainDb
            }
            prefs[PreferencesKeys.BAND_GAINS] = gains.joinToString(",")
            prefs[PreferencesKeys.ACTIVE_PRESET] = "Custom"
        }
    }

    override suspend fun applyPreset(preset: EQPreset) {
        context.eqDataStore.edit { prefs ->
            prefs[PreferencesKeys.ACTIVE_PRESET] = preset.name
            prefs[PreferencesKeys.PREAMP_GAIN] = preset.preampGainDb
            prefs[PreferencesKeys.BAND_GAINS] = preset.bandGainsDb.joinToString(",")
        }
    }

    override suspend fun saveCustomPreset(name: String, preampDb: Float, gains: List<Float>) {
        eqPresetDao.insertPreset(
            EQPresetEntity(
                name = name,
                isBuiltIn = false,
                preampGainDb = preampDb,
                bandGainsJson = gains.joinToString(",")
            )
        )
    }
}
