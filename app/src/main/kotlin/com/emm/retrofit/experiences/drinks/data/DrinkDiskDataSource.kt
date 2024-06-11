package com.emm.retrofit.experiences.drinks.data

import android.content.SharedPreferences
import com.emm.retrofit.core.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DrinkDiskDataSource(
    dispatchers: Dispatchers,
    private val pref: SharedPreferences,
) : DrinkFetcher, DrinkSaver, Dispatchers by dispatchers {

    private val editor get() = pref.edit()

    override fun fetchByName(name: String): Flow<List<DrinkApiModel>> {
        val json: String = pref.getString(name, String()).orEmpty()
        val drinks: List<DrinkApiModel> = drinkToEntity(json)
        return flowOf(drinks)
    }

    override fun saveDrinks(key: String, drinks: List<DrinkApiModel>) {
        if (drinks.isEmpty()) return

        val json: String = drinkToJson(drinks)
        editor.putString(key, json).apply()
    }

    private fun drinkToJson(
        model: List<DrinkApiModel>
    ): String = Json.encodeToString(model)

    private fun drinkToEntity(
        json: String
    ): List<DrinkApiModel> = try {
        Json.decodeFromString<List<DrinkApiModel>>(json)
    } catch (e: Throwable) {
        emptyList()
    }
}