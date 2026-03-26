package com.emm.data.account

import com.emm.data.auth.UserIdProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountRemoteDataSource(
    userIdProvider: UserIdProvider,
    client: SupabaseClient,
) : UserIdProvider by userIdProvider {

    private val table: PostgrestQueryBuilder = client.from("accounts")

    suspend fun upsert(accounts: List<NetworkAccount>) = withContext(Dispatchers.IO) {
        val networkAccounts = accounts.map { it.copy(userId = userId) }
        table.upsert(networkAccounts)
    }

    suspend fun all(): List<NetworkAccount> = withContext(Dispatchers.IO) {
        table.select {
            filter {
                eq("user_id", userId)
            }
        }.decodeList<NetworkAccount>()
    }

    suspend fun delete(accountId: String) {
        table.delete {
            filter {
                eq("account_id", accountId)
            }
        }
    }
}
