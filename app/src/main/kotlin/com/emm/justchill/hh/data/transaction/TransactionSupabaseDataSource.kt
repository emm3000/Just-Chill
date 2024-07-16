package com.emm.justchill.hh.data.transaction

import com.emm.justchill.hh.data.TableNames
import com.emm.justchill.hh.domain.TransactionModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class TransactionSupabaseDataSource(
    supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
) : TransactionNetworkDataSource {

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
                filter {
                    TransactionModel::userId eq userId
                }
            }
            .decodeList<TransactionModel>()
    }
}