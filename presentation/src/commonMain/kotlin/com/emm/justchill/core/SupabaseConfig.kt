package com.emm.justchill.core

/**
 * Platform-provided Supabase connection settings. Android resolves it from BuildConfig
 * (SUPABASE_URL / SUPABASE_ANON_KEY). Injected into the commonMain supabaseModule so the
 * createSupabaseClient builder stays platform-agnostic. The empty-URL -> "http://localhost:54321"
 * fallback is applied platform-side when constructing this, so an absent supabase.properties keeps
 * the app fully usable in anonymous/offline mode.
 */
data class SupabaseConfig(val url: String, val anonKey: String)
