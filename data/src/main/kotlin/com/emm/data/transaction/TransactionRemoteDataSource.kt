package com.emm.data.transaction

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class TransactionRemoteDataSource(client: SupabaseClient) {

    private val table: PostgrestQueryBuilder = client.from("transactions_v2")

    suspend fun upsert(transactions: List<TransactionModel>) {
        table.upsert(transactions)
    }
}