package com.emm.data.account

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountRemoteDataSource(private val client: SupabaseClient) {

    private val table: PostgrestQueryBuilder
        get() = client.from("accounts_v2")

    suspend fun insert(account: AccountModel) = withContext(Dispatchers.IO) {
        table.insert(account)
    }

    suspend fun all(): List<AccountModel> = withContext(Dispatchers.IO) {
        table.select().decodeList<AccountModel>()
    }

    suspend fun delete(accountId: String) {
        table.delete {
            filter {
                eq("account_id", accountId)
            }
        }
    }
}