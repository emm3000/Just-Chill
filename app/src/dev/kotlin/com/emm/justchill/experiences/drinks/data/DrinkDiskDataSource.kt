package com.emm.justchill.experiences.drinks.data

import android.content.SharedPreferences
import com.emm.justchill.core.DispatchersProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DrinkDiskDataSource(
    dispatchersProvider: DispatchersProvider,
    private val pref: SharedPreferences,
) : DrinkFetcher, DrinkSaver, DispatchersProvider by dispatchersProvider {

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