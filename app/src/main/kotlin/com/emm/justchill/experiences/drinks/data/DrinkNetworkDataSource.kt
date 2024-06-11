package com.emm.justchill.experiences.drinks.data

import com.emm.justchill.core.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DrinkNetworkDataSource(
    dispatchers: Dispatchers,
    private val drinkService: DrinkService,
) : DrinkFetcher, Dispatchers by dispatchers {

    override fun fetchByName(name: String): Flow<List<DrinkApiModel>> = flow {
        val drinks: DrinkListApiModel? = drinkService.fetchDrinkByName(name)
        emit(drinks?.drinkApiModelList.orEmpty())
    }.flowOn(ioDispatcher)
}