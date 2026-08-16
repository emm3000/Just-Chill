package com.emm.justchill.hh.di

import com.emm.justchill.core.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val supabaseModule = module {
    single { provideSupabaseClient(get()) }
}

private fun provideSupabaseClient(config: SupabaseConfig): SupabaseClient = createSupabaseClient(
    supabaseUrl = config.url,
    supabaseKey = config.anonKey,
) {
    install(Auth)
    install(Postgrest) {
        @OptIn(SupabaseExperimental::class)
        requireValidSession = true
    }
    install(Storage) {
        @OptIn(SupabaseExperimental::class)
        requireValidSession = true
    }
    defaultSerializer = KotlinXSerializer(
        json = Json {
            ignoreUnknownKeys = true
        },
    )
}
