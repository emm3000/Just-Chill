package com.emm.retrofit.vo

import com.emm.retrofit.domain.DrinkService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object RetrofitClient {

    val drinkService: DrinkService by lazy {
        val loggerInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggerInterceptor)
            .build()

        val networkJson = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        Retrofit.Builder()
            .baseUrl("https://www.thecocktaildb.com/api/json/v1/1/")
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .client(httpClient)
            .build().create(DrinkService::class.java)
    }
}