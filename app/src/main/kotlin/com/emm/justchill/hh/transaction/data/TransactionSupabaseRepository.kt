package com.emm.justchill.hh.transaction.data

import com.emm.justchill.hh.shared.TableNames
import com.emm.justchill.hh.auth.domain.AuthRepository
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
        val userId: String = authRepository.session()?.id ?: return emptyList()
        return client
            .select {
                filter { TransactionModel::userId eq userId }
            }
            .decodeList<TransactionModel>()
    }

    override suspend fun deleteBy(transactionId: String) {
        client.delete {
            filter {
                TransactionModel::transactionId eq transactionId
            }
        }
    }

    override suspend fun deleteAll() {
        val userId: String = authRepository.session()?.id ?: return
        client.delete {
            filter {
                TransactionModel::userId eq userId
            }
        }
    }
}