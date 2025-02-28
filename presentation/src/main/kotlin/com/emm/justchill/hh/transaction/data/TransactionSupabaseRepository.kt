package com.emm.justchill.hh.transaction.data

import com.emm.justchill.hh.shared.TableNames
import com.emm.domain.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class TransactionSupabaseRepository(
    supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
) : TransactionRemoteRepository {

    private val client: PostgrestQueryBuilder by lazy {
        supabaseClient.from(TableNames.TRANSACTION_TABLE)
    }

    override suspend fun upsert(transaction: TransactionModel) {
        client.upsert(transaction)
    }

    override suspend fun upsert(transactions: List<TransactionModel>) {
        client.upsert(transactions)
    }

    override suspend fun retrieve(): List<TransactionModel> {
        return emptyList()
    }

    override suspend fun deleteBy(transactionId: String) {
        client.delete {
            filter {
                TransactionModel::transactionId eq transactionId
            }
        }
    }

    override suspend fun deleteAll() {

    }
}