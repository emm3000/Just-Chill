package com.emm.justchill.core

data class SupabaseConfig(val url: String, val anonKey: String) {

    companion object {
        // createSupabaseClient refuses a blank URL, and a build without credentials must stay usable
        // offline and without an account (PRD §2 Offline).
        private const val OFFLINE_URL: String = "http://localhost:54321"

        fun withOfflineFallback(url: String, anonKey: String): SupabaseConfig =
            SupabaseConfig(url = url.ifBlank { OFFLINE_URL }, anonKey = anonKey)
    }
}
