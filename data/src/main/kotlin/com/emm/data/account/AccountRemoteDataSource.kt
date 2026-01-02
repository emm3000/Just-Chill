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

    suspend fun upsert(accounts: List<AccountModel>) = withContext(Dispatchers.IO) {
        val accountModels = accounts.map(::withUserId)
        table.upsert(accountModels)
    }

    private fun withUserId(accountModel: AccountModel) = accountModel.copy(userId = userId)

    suspend fun all(): List<AccountModel> = withContext(Dispatchers.IO) {
        table.select {
            filter {
                eq("user_id", userId)
            }
        }.decodeList<AccountModel>()
    }

    suspend fun delete(accountId: String) {
        table.delete {
            filter {
                eq("account_id", accountId)
            }
        }
    }
}