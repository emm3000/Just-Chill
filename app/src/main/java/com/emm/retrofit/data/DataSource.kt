package com.emm.retrofit.data

import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.vo.Resource
import com.emm.retrofit.vo.RetrofitClient

class DataSource {

    suspend fun getTragoByName(tragoName: String): Resource<List<Drink>> {
        return Resource.Success(RetrofitClient.webService.getTragoByName(tragoName).drinkList)
    }


}