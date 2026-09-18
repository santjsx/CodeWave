package com.codewave.player.core.audio

import android.content.Context
import com.codewave.player.core.model.AutoEqModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class AutoEqManager(private val context: Context) {

    private var cachedModels: List<AutoEqModel>? = null

    suspend fun getModels(brandFilter: String? = null, query: String = ""): List<AutoEqModel> =
        withContext(Dispatchers.IO) {
            val all = getAllModels()
            val filteredByBrand = if (brandFilter.isNullOrBlank() || brandFilter.equals("All", ignoreCase = true)) {
                all
            } else {
                all.filter { it.brand.equals(brandFilter, ignoreCase = true) }
            }

            if (query.isBlank()) {
                filteredByBrand
            } else {
                val q = query.trim().lowercase()
                filteredByBrand.filter {
                    it.brand.lowercase().contains(q) ||
                    it.model.lowercase().contains(q) ||
                    it.type.lowercase().contains(q) ||
                    it.source.lowercase().contains(q)
                }
            }
        }

    suspend fun getBrands(): List<String> = withContext(Dispatchers.IO) {
        val all = getAllModels()
        listOf("All") + all.map { it.brand }.distinct().sorted()
    }

    suspend fun getModelById(id: String): AutoEqModel? = withContext(Dispatchers.IO) {
        getAllModels().find { it.id == id }
    }

    private suspend fun getAllModels(): List<AutoEqModel> {
        cachedModels?.let { return it }

        return try {
            val jsonString = context.assets.open("autoeq/models.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            }
            val jsonArray = JSONArray(jsonString)
            val list = ArrayList<AutoEqModel>(jsonArray.length())

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val brand = obj.getString("brand")
                val model = obj.getString("model")
                val type = obj.optString("type", "Headphone")
                val source = obj.optString("source", "oratory1990")
                val preampDb = obj.optDouble("preampDb", 0.0).toFloat()

                val gainsArray = obj.getJSONArray("gains")
                val gains = ArrayList<Float>(gainsArray.length())
                for (g in 0 until gainsArray.length()) {
                    gains.add(gainsArray.getDouble(g).toFloat())
                }

                list.add(
                    AutoEqModel(
                        id = id,
                        brand = brand,
                        model = model,
                        type = type,
                        source = source,
                        preampDb = preampDb,
                        gains = gains
                    )
                )
            }

            val sorted = list.sortedWith(compareBy({ it.brand }, { it.model }))
            cachedModels = sorted
            sorted
        } catch (e: Exception) {
            emptyList()
        }
    }
}
