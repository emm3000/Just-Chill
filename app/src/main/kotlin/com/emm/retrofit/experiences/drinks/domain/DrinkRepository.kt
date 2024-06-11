package com.emm.retrofit.experiences.drinks.domain

import com.emm.retrofit.experiences.drinks.data.DrinkApiModel
import kotlinx.coroutines.flow.Flow

interface DrinkRepository {

    fun fetchByName(name: String): Flow<List<DrinkApiModel>>
}