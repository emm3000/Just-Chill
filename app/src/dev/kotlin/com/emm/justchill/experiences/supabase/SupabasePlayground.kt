package com.emm.justchill.experiences.supabase

import android.util.Log
import com.emm.justchill.hh.data.shared.TableNames
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabasePlayground(
    private val supabaseClient: SupabaseClient,
) {

    suspend fun add() = withContext(Dispatchers.IO) {
        val data = supabaseClient
            .from(TableNames.TRANSACTION_TABLE)
            .select(Columns.raw("transaction_id, user_id, categories(category_id, user_id)"))

        Log.e("aea", data.data)
    }
}