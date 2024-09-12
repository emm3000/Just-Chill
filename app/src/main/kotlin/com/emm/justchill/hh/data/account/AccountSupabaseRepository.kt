package com.emm.justchill.hh.data.account

import com.emm.justchill.hh.data.shared.TableNames
import com.emm.justchill.hh.domain.auth.AuthRepository
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
        val userId: String = authRepository.session()?.id ?: return emptyList()
        return client
            .select {
                filter { AccountModel::userId eq userId }
            }
            .decodeList<AccountModel>()
    }

    override suspend fun deleteBy(accountId: String) {
        client.delete {
            filter {
                AccountModel::accountId eq accountId
            }
        }
    }

    override suspend fun deleteAll() {
        val userId: String = authRepository.session()?.id ?: return
        client.delete {
            filter {
                AccountModel::userId eq userId
            }
        }
    }
}