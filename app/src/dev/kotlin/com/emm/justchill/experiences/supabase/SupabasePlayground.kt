package com.emm.justchill.experiences.supabase

import com.emm.justchill.hh.domain.AndroidDataProvider
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabasePlayground(
    private val supabaseClient: SupabaseClient,
    private val androidDataProvider: AndroidDataProvider,
) {

    suspend fun add() = withContext(Dispatchers.IO) {

    }
}