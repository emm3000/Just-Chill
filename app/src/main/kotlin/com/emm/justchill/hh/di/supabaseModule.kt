package com.emm.justchill.hh.di

import android.app.Application
import com.emm.justchill.R
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val supabaseModule = module {

    single { provideSupabaseClient(androidApplication()) }
}

private fun provideSupabaseClient(application: Application): SupabaseClient = createSupabaseClient(
    supabaseUrl = application.getString(R.string.supabase_url),
    supabaseKey = application.getString(R.string.supabase_key)
) {
    install(Auth)
    install(Postgrest)
    defaultSerializer = KotlinXSerializer(
        json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    )
}