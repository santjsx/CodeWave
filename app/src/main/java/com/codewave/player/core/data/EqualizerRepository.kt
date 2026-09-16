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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.eqDataStore by preferencesDataStore(name = "codewave_eq_prefs")

interface EqualizerRepository {
    val equalizerConfig: StateFlow<EqualizerConfig>
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

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var preampPersistJob: Job? = null
    private var bandGainPersistJob: Job? = null

    private object PreferencesKeys {
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val PREAMP_GAIN = floatPreferencesKey("preamp_gain")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val ACTIVE_PRESET = stringPreferencesKey("active_preset")
        val BAND_GAINS = stringPreferencesKey("band_gains")
    }

    private val _equalizerConfig = MutableStateFlow(EqualizerConfig())
    override val equalizerConfig: StateFlow<EqualizerConfig> = _equalizerConfig.asStateFlow()

    init {
        repoScope.launch {
            val prefs = context.eqDataStore.data.first()
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

            _equalizerConfig.value = EqualizerConfig(
                isEnabled = enabled,
                preampGainDb = preamp,
                isLimiterEnabled = limiter,
                activePresetName = preset,
                bands = bands
            )
        }
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
        _equalizerConfig.update { it.copy(isEnabled = enabled) }
        repoScope.launch {
            context.eqDataStore.edit { it[PreferencesKeys.EQ_ENABLED] = enabled }
        }
    }

    override suspend fun setPreampGain(gainDb: Float) {
        _equalizerConfig.update { it.copy(preampGainDb = gainDb) }
        preampPersistJob?.cancel()
        preampPersistJob = repoScope.launch {
            delay(250)
            context.eqDataStore.edit { it[PreferencesKeys.PREAMP_GAIN] = gainDb }
        }
    }

    override suspend fun setLimiterEnabled(enabled: Boolean) {
        _equalizerConfig.update { it.copy(isLimiterEnabled = enabled) }
        repoScope.launch {
            context.eqDataStore.edit { it[PreferencesKeys.LIMITER_ENABLED] = enabled }
        }
    }

    override suspend fun setBandGain(bandIndex: Int, gainDb: Float) {
        _equalizerConfig.update { current ->
            val updatedBands = current.bands.mapIndexed { index, band ->
                if (index == bandIndex) band.copy(gainDb = gainDb) else band
            }
            current.copy(
                activePresetName = "Custom",
                bands = updatedBands
            )
        }
        bandGainPersistJob?.cancel()
        bandGainPersistJob = repoScope.launch {
            delay(250)
            val gains = _equalizerConfig.value.bands.map { it.gainDb }
            context.eqDataStore.edit { prefs ->
                prefs[PreferencesKeys.BAND_GAINS] = gains.joinToString(",")
                prefs[PreferencesKeys.ACTIVE_PRESET] = "Custom"
            }
        }
    }

    override suspend fun applyPreset(preset: EQPreset) {
        val defaultBands = EqualizerConfig.defaultBands()
        val updatedBands = defaultBands.mapIndexed { index, band ->
            band.copy(gainDb = preset.bandGainsDb.getOrElse(index) { 0f })
        }
        _equalizerConfig.update { current ->
            current.copy(
                activePresetName = preset.name,
                preampGainDb = preset.preampGainDb,
                bands = updatedBands
            )
        }
        preampPersistJob?.cancel()
        bandGainPersistJob?.cancel()
        repoScope.launch {
            context.eqDataStore.edit { prefs ->
                prefs[PreferencesKeys.ACTIVE_PRESET] = preset.name
                prefs[PreferencesKeys.PREAMP_GAIN] = preset.preampGainDb
                prefs[PreferencesKeys.BAND_GAINS] = preset.bandGainsDb.joinToString(",")
            }
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
