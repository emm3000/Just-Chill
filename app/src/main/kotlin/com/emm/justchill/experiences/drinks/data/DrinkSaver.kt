package com.emm.justchill.experiences.drinks.data

interface DrinkSaver {

    fun saveDrinks(key: String, drinks: List<DrinkApiModel>)
}