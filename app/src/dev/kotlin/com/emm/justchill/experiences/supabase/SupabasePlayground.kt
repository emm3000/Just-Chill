package com.emm.justchill.experiences.supabase

import com.emm.justchill.hh.domain.AndroidIdProvider
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabasePlayground(
    private val supabaseClient: SupabaseClient,
    private val androidIdProvider: AndroidIdProvider,
) {

    suspend fun add() = withContext(Dispatchers.IO) {

    }
}