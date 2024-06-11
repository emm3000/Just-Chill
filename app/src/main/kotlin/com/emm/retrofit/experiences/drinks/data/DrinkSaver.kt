package com.emm.retrofit.experiences.drinks.data

interface DrinkSaver {

    fun saveDrinks(key: String, drinks: List<DrinkApiModel>)
}