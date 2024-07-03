package com.emm.justchill.experiences.drinks.domain

import com.emm.justchill.experiences.drinks.data.DrinkApiModel
import kotlinx.coroutines.flow.Flow

interface DrinkRepository {

    fun fetchByName(name: String): Flow<List<DrinkApiModel>>
}