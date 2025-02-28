package com.emm.justchill.hh.account.data

import com.emm.justchill.hh.shared.TableNames
import com.emm.domain.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class AccountSupabaseRepository(
    supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
) : AccountRemoteRepository {

    private val client: PostgrestQueryBuilder by lazy {
        supabaseClient.from(TableNames.ACCOUNT_TABLE)
    }

    override suspend fun upsert(account: AccountModel) {
        client.upsert(account)
    }

    override suspend fun retrieve(): List<AccountModel> {
        return emptyList()
    }

    override suspend fun deleteBy(accountId: String) {
        client.delete {
            filter {
                AccountModel::accountId eq accountId
            }
        }
    }

    override suspend fun deleteAll() {

    }
}