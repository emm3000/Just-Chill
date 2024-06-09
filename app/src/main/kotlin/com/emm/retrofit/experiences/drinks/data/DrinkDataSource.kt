package com.emm.retrofit.experiences.drinks.data

import kotlinx.coroutines.flow.Flow

interface DrinkDataSource {

    fun fetchDrinksByName(drinkName: String): Flow<List<DrinkApiModel>>
}