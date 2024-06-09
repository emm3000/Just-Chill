package com.emm.retrofit.experiences.drinks.data

import com.emm.retrofit.core.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DrinkNetworkDataSource(
    dispatchers: Dispatchers,
    private val drinkService: DrinkService,
) : DrinkDataSource, Dispatchers by dispatchers {

    override fun fetchDrinksByName(drinkName: String): Flow<List<DrinkApiModel>> = flow {
        val drinks: DrinkListApiModel = drinkService.fetchDrinkByName(drinkName)
        emit(drinks.drinkApiModelList)
    }.flowOn(ioDispatcher)
}