package com.codewave.player.core.model

data class AutoEqModel(
    val id: String,
    val brand: String,
    val model: String,
    val type: String, // e.g., "In-Ear (IEM)", "Over-Ear", "Wireless ANC"
    val source: String, // e.g., "oratory1990", "crinacle", "rtings", "Harman Target"
    val preampDb: Float,
    val gains: List<Float> // 10 bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
) {
    val displayName: String
        get() = "$brand $model"

    fun toEQPreset(): EQPreset {
        return EQPreset(
            id = System.currentTimeMillis() xor id.hashCode().toLong(),
            name = "$brand $model (AutoEq)",
            isBuiltIn = false,
            preampGainDb = preampDb,
            bandGainsDb = gains
        )
    }
}
