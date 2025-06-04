package com.emm.data.transaction

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRemoteDataSource(client: SupabaseClient) {

    private val table: PostgrestQueryBuilder = client.from("transactions_v2")

    suspend fun upsert(transactions: List<TransactionModel>) {
        table.upsert(transactions)
    }

    suspend fun all(): List<TransactionModel> = withContext(Dispatchers.IO) {
        table.select().decodeList<TransactionModel>()
    }
}