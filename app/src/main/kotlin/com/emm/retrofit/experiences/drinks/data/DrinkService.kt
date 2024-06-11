package com.emm.retrofit.experiences.drinks.data

import retrofit2.http.GET
import retrofit2.http.Query

interface DrinkService {

    @GET("search.php")
    suspend fun fetchDrinkByName(@Query("s") drink: String) : DrinkListApiModel?
}