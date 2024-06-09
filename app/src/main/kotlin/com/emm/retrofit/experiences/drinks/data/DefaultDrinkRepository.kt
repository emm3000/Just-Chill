package com.emm.retrofit.experiences.drinks.data

import com.emm.retrofit.experiences.drinks.domain.DrinkRepository
import kotlinx.coroutines.flow.Flow

class DefaultDrinkRepository(
    private val drinkNetworkDataSource: DrinkDataSource,
): DrinkRepository {

    override fun fetchByName(name: String): Flow<List<DrinkApiModel>> {
        return drinkNetworkDataSource.fetchDrinksByName(name)
    }
}