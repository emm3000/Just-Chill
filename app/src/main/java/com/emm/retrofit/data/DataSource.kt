package com.emm.retrofit.data

import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.data.model.DrinkList
import com.emm.retrofit.domain.WebService
import com.emm.retrofit.vo.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DataSource {

    fun fetchDrinkByName(drinkName: String): Flow<List<Drink>> = flow {
        val retrofitService: WebService = RetrofitClient.service
        val drinks: DrinkList = retrofitService.fetchDrinkByName(drinkName)
        emit(drinks.drinkList)
    }.flowOn(Dispatchers.IO)
}