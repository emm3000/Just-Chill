package com.emm.retrofit.data

import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.data.model.DrinkList
import com.emm.retrofit.domain.WebService
import com.emm.retrofit.vo.Result
import com.emm.retrofit.vo.RetrofitClient

class DataSource {

    suspend fun fetchDrinkByName(drinkName: String): Result<List<Drink>> {
        val retrofitService: WebService = RetrofitClient.service
        val drinks: DrinkList = retrofitService.fetchDrinkByName(drinkName)
        return Result.Success(drinks.drinkList)
    }
}