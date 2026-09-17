package com.emm.justchill.core

// Injected into supabaseModule from :androidApp's BuildConfig: :presentation cannot reach it
// directly, the dependency graph runs the other way.
data class SupabaseConfig(val url: String, val anonKey: String)
