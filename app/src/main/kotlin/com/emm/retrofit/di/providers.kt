package com.emm.retrofit.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

fun provideDrinkService(): Retrofit {

    val loggerInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()
        .setLevel(HttpLoggingInterceptor.Level.BODY)

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggerInterceptor)
        .build()

    val networkJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    return Retrofit.Builder()
        .baseUrl("https://www.thecocktaildb.com/api/json/v1/1/")
        .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
        .client(httpClient)
        .build()
}