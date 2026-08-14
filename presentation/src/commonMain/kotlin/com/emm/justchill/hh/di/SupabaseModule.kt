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

// Single commonMain Supabase client (replaces :androidApp/hh/di/SupabaseModule.kt + KoinIos.kt's
// iosSupabaseModule). URL/key come from the injected platform SupabaseConfig (Android BuildConfig /
// iOS IosSupabaseConfig). The builder body is byte-identical to the previous platform copies except
// the config source.
val supabaseModule = module {
    single { provideSupabaseClient(get()) }
}

private fun provideSupabaseClient(config: SupabaseConfig): SupabaseClient {
    // createSupabaseClient requires a non-blank URL; the empty -> "http://localhost:54321" fallback is
    // applied platform-side when constructing SupabaseConfig. Network calls then fail and surface as
    // DomainException.NetworkUnavailable — the app remains fully usable in anonymous/offline mode.
    return createSupabaseClient(
        supabaseUrl = config.url,
        supabaseKey = config.anonKey,
    ) {
        install(Auth)
        install(Postgrest) {
            // Require an authenticated session for every Postgrest request: never fall back to the
            // anon supabaseKey. Postgrest is used ONLY by the sync engine and the delete_account RPC,
            // both strictly authenticated paths. Without this, an unresolved JWT is silently
            // downgraded to an anonymous request that RLS rejects with a confusing HTTP 403; with it,
            // the call throws SessionRequiredException, which maps to a retryable sync error instead.
            @OptIn(SupabaseExperimental::class)
            requireValidSession = true
        }
        install(Storage) {
            // Same strict-auth posture as Postgrest above, and it is the same knob: Storage.Config
            // implements AuthDependentPluginConfig, so `requireValidSession` exists here too and
            // defaults to false (verified against 3.7.0, not assumed). Storage holds one thing —
            // per-user snapshot backups behind RLS (ADR 009) — and ADR 009 Decision 1 says no
            // session means no pipeline, never a queued upload. Left at the default, an unresolved
            // JWT would be downgraded to an anonymous request that RLS answers with a confusing
            // 403; with it, the call throws SessionRequiredException, which is a distinct and
            // reportable reason (hard constraint 4: no silent failure).
            @OptIn(SupabaseExperimental::class)
            requireValidSession = true
        }
        defaultSerializer = KotlinXSerializer(
            json = Json {
                ignoreUnknownKeys = true
            },
        )
    }
}
