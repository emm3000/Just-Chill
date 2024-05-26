package com.emm.retrofit.domain

import com.emm.retrofit.data.model.DrinkList
import retrofit2.http.GET
import retrofit2.http.Query

interface WebService {

    @GET("search.php")
    suspend fun fetchDrinkByName(@Query("s") drink: String) : DrinkList
}