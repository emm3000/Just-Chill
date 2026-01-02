package com.emm.data.transaction

import com.emm.data.auth.UserIdProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRemoteDataSource(
    client: SupabaseClient,
    private val userIdProvider: UserIdProvider,
) {

    private val table: PostgrestQueryBuilder = client.from("transactions")

    suspend fun upsert(transactions: List<TransactionModel>) = withContext(Dispatchers.IO) {
        val transactionModels: List<TransactionModel> = transactions.map { it.copy(userId = userIdProvider.userId) }
        table.upsert(transactionModels)
    }

    suspend fun all(accounts: List<String>): List<TransactionModel> = withContext(Dispatchers.IO) {
        table.select {
            filter {
                isIn("account_id", accounts)
            }
        }.decodeList<TransactionModel>()
    }
}