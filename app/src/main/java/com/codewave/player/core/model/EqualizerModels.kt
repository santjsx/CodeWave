package com.codewave.player.core.model

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val gainDb: Float, // Typically -12.0f to +12.0f dB
    val minGainDb: Float = -12.0f,
    val maxGainDb: Float = 12.0f
) {
    val frequencyLabel: String
        get() = if (centerFreqHz >= 1000) {
            val khz = centerFreqHz / 1000
            val remainder = (centerFreqHz % 1000) / 100
            if (remainder > 0) "${khz}.${remainder}k" else "${khz}k"
        } else {
            "$centerFreqHz"
        }
}

data class EQPreset(
    val id: Long,
    val name: String,
    val isBuiltIn: Boolean,
    val preampGainDb: Float,
    val bandGainsDb: List<Float>
) {
    companion object {
        val PRESETS_10_BAND = listOf(
            EQPreset(1, "Flat", true, 0f, listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EQPreset(2, "Acoustic", true, -1f, listOf(3f, 3f, 2f, 1f, 1f, 1f, 2f, 2f, 3f, 2f)),
            EQPreset(3, "Bass Boost", true, -3f, listOf(6f, 5f, 4f, 2f, 1f, 0f, 0f, 0f, 0f, 0f)),
            EQPreset(4, "Bass Reducer", true, 0f, listOf(-5f, -4f, -3f, -2f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EQPreset(5, "Classical", true, -1.5f, listOf(4f, 3f, 2f, 2f, -1f, -1f, 0f, 2f, 3f, 3f)),
            EQPreset(6, "Dance", true, -2.5f, listOf(5f, 4f, 2f, 0f, 0f, 2f, 3f, 4f, 4f, 0f)),
            EQPreset(7, "Electronic", true, -2.5f, listOf(4f, 3.5f, 1f, 0f, -1.5f, 2f, 1f, 2f, 4f, 4.5f)),
            EQPreset(8, "Hip-Hop", true, -3f, listOf(6f, 5f, 2f, 1.5f, -1f, -1f, 1f, -1f, 2f, 3f)),
            EQPreset(9, "Jazz", true, -1f, listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)),
            EQPreset(10, "Pop", true, -1.5f, listOf(-1.5f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -1.5f)),
            EQPreset(11, "Rock", true, -2.5f, listOf(5f, 3f, -1f, -2f, -0.5f, 2f, 3.5f, 4f, 4f, 4f)),
            EQPreset(12, "Vocal Clarity", true, -1.5f, listOf(-2f, -2f, -1f, 1f, 3f, 3.5f, 3f, 2f, 1f, 0f))
        )
    }
}

data class EqualizerConfig(
    val isEnabled: Boolean = false,
    val preampGainDb: Float = 0f,
    val isLimiterEnabled: Boolean = true,
    val activePresetName: String = "Flat",
    val bands: List<EqualizerBand> = defaultBands()
) {
    companion object {
        fun defaultBands(): List<EqualizerBand> {
            val defaultFreqs = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
            return defaultFreqs.mapIndexed { index, freq ->
                EqualizerBand(index, freq, 0f)
            }
        }
    }
}
