package com.emm.justchill.experiences.drinks.data

import kotlinx.coroutines.flow.Flow

interface DrinkFetcher {

    fun fetchByName(name: String): Flow<List<DrinkApiModel>>
}