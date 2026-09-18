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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    suspend fun deletePreset(preset: EQPreset)
    suspend fun resetPresetsToDefaults()
    suspend fun setBassEnabled(enabled: Boolean)
    suspend fun setBassGain(gainDb: Float)
    suspend fun setClarityEnabled(enabled: Boolean)
    suspend fun setClarityGain(gainDb: Float)
    suspend fun setConvolverEnabled(enabled: Boolean)
    suspend fun setIrsName(name: String?)
    suspend fun setVocalRemoverEnabled(enabled: Boolean)
    suspend fun setVocalRemoverLevel(level: Float)
}

class DefaultEqualizerRepository(
    private val context: Context,
    private val eqPresetDao: EQPresetDao
) : EqualizerRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val seedingMutex = Mutex()
    private var preampPersistJob: Job? = null
    private var bandGainPersistJob: Job? = null
    private var bassPersistJob: Job? = null
    private var clarityPersistJob: Job? = null

    private object PreferencesKeys {
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val PREAMP_GAIN = floatPreferencesKey("preamp_gain")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val ACTIVE_PRESET = stringPreferencesKey("active_preset")
        val BAND_GAINS = stringPreferencesKey("band_gains")
        val BASS_ENABLED = booleanPreferencesKey("bass_enabled")
        val BASS_GAIN = floatPreferencesKey("bass_gain")
        val CLARITY_ENABLED = booleanPreferencesKey("clarity_enabled")
        val CLARITY_GAIN = floatPreferencesKey("clarity_gain")
        val CONVOLVER_ENABLED = booleanPreferencesKey("convolver_enabled")
        val IRS_NAME = stringPreferencesKey("irs_name")
        val VOCAL_REMOVER_ENABLED = booleanPreferencesKey("vocal_remover_enabled")
        val VOCAL_REMOVER_LEVEL = floatPreferencesKey("vocal_remover_level")
    }

    private val _equalizerConfig = MutableStateFlow(EqualizerConfig())
    override val equalizerConfig: StateFlow<EqualizerConfig> = _equalizerConfig.asStateFlow()

    init {
        repoScope.launch {
            ensureDefaultPresetsSeeded()
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

            val bassEnabled = prefs[PreferencesKeys.BASS_ENABLED] ?: false
            val bassGain = prefs[PreferencesKeys.BASS_GAIN] ?: 4.0f
            val clarityEnabled = prefs[PreferencesKeys.CLARITY_ENABLED] ?: false
            val clarityGain = prefs[PreferencesKeys.CLARITY_GAIN] ?: 3.0f
            val convolverEnabled = prefs[PreferencesKeys.CONVOLVER_ENABLED] ?: false
            val irsName = prefs[PreferencesKeys.IRS_NAME]
            val vocalRemoverEnabled = prefs[PreferencesKeys.VOCAL_REMOVER_ENABLED] ?: false
            val vocalRemoverLevel = prefs[PreferencesKeys.VOCAL_REMOVER_LEVEL] ?: 0.85f

            _equalizerConfig.value = EqualizerConfig(
                isEnabled = enabled,
                preampGainDb = preamp,
                isLimiterEnabled = limiter,
                activePresetName = preset,
                bands = bands,
                isBassEnabled = bassEnabled,
                bassGainDb = bassGain,
                isClarityEnabled = clarityEnabled,
                clarityGainDb = clarityGain,
                isConvolverEnabled = convolverEnabled,
                irsName = irsName,
                isVocalRemoverEnabled = vocalRemoverEnabled,
                vocalRemoverLevel = vocalRemoverLevel
            )
        }
    }

    private suspend fun ensureDefaultPresetsSeeded() {
        seedingMutex.withLock {
            eqPresetDao.deduplicatePresets()
            val count = eqPresetDao.getBuiltInPresetCount()
            if (count < EQPreset.PRESETS_10_BAND.size) {
                val existing = eqPresetDao.getAllPresets()
                val existingNames = existing.filter { it.isBuiltIn }.map { it.name.lowercase() }.toSet()
                val missingEntities = EQPreset.PRESETS_10_BAND
                    .filter { it.name.lowercase() !in existingNames }
                    .map { preset ->
                        EQPresetEntity(
                            id = preset.id,
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

    override val presets: Flow<List<EQPreset>> = eqPresetDao.getAllPresetsFlow().map { entities ->
        if (entities.isEmpty()) {
            EQPreset.PRESETS_10_BAND
        } else {
            val dbPresets = entities.map { entity ->
                val gains = entity.bandGainsJson.split(",").mapNotNull { it.toFloatOrNull() }
                EQPreset(
                    id = entity.id,
                    name = entity.name,
                    isBuiltIn = entity.isBuiltIn,
                    preampGainDb = entity.preampGainDb,
                    bandGainsDb = gains
                )
            }
            // Ensure any missing built-in presets are always present in the emitted list
            val dbBuiltInNames = dbPresets.filter { it.isBuiltIn }.map { it.name.lowercase() }.toSet()
            val missingBuiltIns = EQPreset.PRESETS_10_BAND.filter { it.name.lowercase() !in dbBuiltInNames }
            val combined = (dbPresets + missingBuiltIns).distinctBy { it.name.lowercase() }
            combined.sortedWith(compareByDescending<EQPreset> { it.isBuiltIn }.thenBy { it.name })
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
        val trimmedName = name.trim()
        val existing = eqPresetDao.getAllPresets().find { it.name.equals(trimmedName, ignoreCase = true) && !it.isBuiltIn }
        eqPresetDao.insertPreset(
            EQPresetEntity(
                id = existing?.id ?: 0L,
                name = trimmedName,
                isBuiltIn = false,
                preampGainDb = preampDb,
                bandGainsJson = gains.joinToString(",")
            )
        )
    }

    override suspend fun deletePreset(preset: EQPreset) {
        if (!preset.isBuiltIn) {
            eqPresetDao.deletePresetById(preset.id)
            if (_equalizerConfig.value.activePresetName == preset.name) {
                val flat = EQPreset.PRESETS_10_BAND.first()
                applyPreset(flat)
            }
        }
    }

    override suspend fun resetPresetsToDefaults() {
        seedingMutex.withLock {
            eqPresetDao.deleteBuiltInPresets()
            val entities = EQPreset.PRESETS_10_BAND.map { preset ->
                EQPresetEntity(
                    id = preset.id,
                    name = preset.name,
                    isBuiltIn = preset.isBuiltIn,
                    preampGainDb = preset.preampGainDb,
                    bandGainsJson = preset.bandGainsDb.joinToString(",")
                )
            }
            eqPresetDao.insertPresets(entities)
        }
    }

    override suspend fun setBassEnabled(enabled: Boolean) {
        _equalizerConfig.update { it.copy(isBassEnabled = enabled) }
        repoScope.launch {
            context.eqDataStore.edit { it[PreferencesKeys.BASS_ENABLED] = enabled }
        }
    }

    override suspend fun setBassGain(gainDb: Float) {
        _equalizerConfig.update { it.copy(bassGainDb = gainDb) }
        bassPersistJob?.cancel()
        bassPersistJob = repoScope.launch {
            delay(250)
            context.eqDataStore.edit { it[PreferencesKeys.BASS_GAIN] = gainDb }
        }
    }

    override suspend fun setClarityEnabled(enabled: Boolean) {
        _equalizerConfig.update { it.copy(isClarityEnabled = enabled) }
        repoScope.launch {
            context.eqDataStore.edit { it[PreferencesKeys.CLARITY_ENABLED] = enabled }
        }
    }

    override suspend fun setClarityGain(gainDb: Float) {
        _equalizerConfig.update { it.copy(clarityGainDb = gainDb) }
        clarityPersistJob?.cancel()
        clarityPersistJob = repoScope.launch {
            delay(250)
            context.eqDataStore.edit { it[PreferencesKeys.CLARITY_GAIN] = gainDb }
        }
    }

    override suspend fun setConvolverEnabled(enabled: Boolean) {
        _equalizerConfig.update { it.copy(isConvolverEnabled = enabled) }
        repoScope.launch {
            context.eqDataStore.edit { it[PreferencesKeys.CONVOLVER_ENABLED] = enabled }
        }
    }

    override suspend fun setIrsName(name: String?) {
        _equalizerConfig.update { it.copy(irsName = name) }
        repoScope.launch {
            context.eqDataStore.edit { prefs ->
                if (name != null) {
                    prefs[PreferencesKeys.IRS_NAME] = name
                } else {
                    prefs.remove(PreferencesKeys.IRS_NAME)
                }
            }
        }
    }

    override suspend fun setVocalRemoverEnabled(enabled: Boolean) {
        _equalizerConfig.update { it.copy(isVocalRemoverEnabled = enabled) }
        repoScope.launch {
            context.eqDataStore.edit { it[PreferencesKeys.VOCAL_REMOVER_ENABLED] = enabled }
        }
    }

    override suspend fun setVocalRemoverLevel(level: Float) {
        val clamped = level.coerceIn(0.0f, 1.0f)
        _equalizerConfig.update { it.copy(vocalRemoverLevel = clamped) }
        repoScope.launch {
            context.eqDataStore.edit { it[PreferencesKeys.VOCAL_REMOVER_LEVEL] = clamped }
        }
    }
}
