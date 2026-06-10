package com.emm.justchill.hh.di

import com.emm.justchill.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val supabaseModule = module {
    single { provideSupabaseClient() }
}

private fun provideSupabaseClient(): SupabaseClient {
    // When supabase.properties is absent (CI, prod today) the BuildConfig fields are empty strings.
    // createSupabaseClient requires a non-blank URL, so we substitute a local placeholder.
    // Network calls will fail and surface as DomainException.NetworkUnavailable — the app remains
    // fully usable in anonymous/offline mode. This placeholder is intentionally non-functional.
    val url = BuildConfig.SUPABASE_URL.ifBlank { "http://localhost:54321" }
    val key = BuildConfig.SUPABASE_ANON_KEY

    return createSupabaseClient(
        supabaseUrl = url,
        supabaseKey = key,
    ) {
        install(Auth)
        install(Postgrest)
        defaultSerializer = KotlinXSerializer(
            json = Json {
                ignoreUnknownKeys = true
            },
        )
    }
}
