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

    suspend fun upsert(transactions: List<NetworkTransaction>) = withContext(Dispatchers.IO) {
        val networkTransactions: List<NetworkTransaction> = transactions.map { it.copy(userId = userIdProvider.userId) }
        table.upsert(networkTransactions)
    }

    suspend fun all(accounts: List<String>): List<NetworkTransaction> = withContext(Dispatchers.IO) {
        table.select {
            filter {
                isIn("account_id", accounts)
            }
        }.decodeList<NetworkTransaction>()
    }

    suspend fun deleteMultipleRows(transactionIds: List<String>) = withContext(Dispatchers.IO) {
        table.delete {
            filter {
                isIn("transaction_id", transactionIds)
            }
        }
    }
}
