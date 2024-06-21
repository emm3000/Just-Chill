package com.emm.justchill.experiences.drinks.data

import com.emm.justchill.core.DispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DrinkNetworkDataSource(
    dispatchersProvider: DispatchersProvider,
    private val drinkService: DrinkService,
) : DrinkFetcher, DispatchersProvider by dispatchersProvider {

    override fun fetchByName(name: String): Flow<List<DrinkApiModel>> = flow {
        val drinks: DrinkListApiModel? = drinkService.fetchDrinkByName(name)
        emit(drinks?.drinkApiModelList.orEmpty())
    }.flowOn(ioDispatcher)
}