package com.emm.justchill.hh.data.transaction

import com.emm.justchill.hh.domain.TransactionModel
import com.emm.justchill.hh.domain.AndroidIdProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class TransactionSupabaseDataSource(
    supabaseClient: SupabaseClient,
    androidIdProvider: AndroidIdProvider,
): TransactionNetworkDataSource {

    private val client: PostgrestQueryBuilder by lazy {
        supabaseClient.from(TABLE_NAME)
    }

    private val androidId = androidIdProvider.androidId()

    override suspend fun upsert(transaction: TransactionModel) {
        client.upsert(transaction)
    }

    override suspend fun upsert(transactions: List<TransactionModel>) {
        client.upsert(transactions)
    }

    override suspend fun retrieve(): List<TransactionModel> {
        return client
            .select {
                filter {
                    TransactionModel::deviceId eq androidId
                }
            }
            .decodeList<TransactionModel>()
    }

    private companion object {

        const val TABLE_NAME = "transaction"
    }
}