package com.emm.justchill.hh.data.transactioncategory

import com.emm.justchill.hh.domain.TransactionCategoryModel
import com.emm.justchill.hh.domain.TransactionModel
import com.emm.justchill.hh.domain.AndroidDataProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class TransactionCategorySupabaseDataSource(
    supabaseClient: SupabaseClient,
    androidDataProvider: AndroidDataProvider,
) : TransactionCategoryNetworkDataSource {

    private val client: PostgrestQueryBuilder by lazy {
        supabaseClient.from(TABLE_NAME)
    }

    private val androidId = androidDataProvider.androidId()

    override suspend fun upsert(transactionCategory: TransactionCategoryModel) {
        client.upsert(transactionCategory)
    }

    override suspend fun upsert(transactionsCategories: List<TransactionCategoryModel>) {
        client.upsert(transactionsCategories)
    }

    override suspend fun retrieve(): List<TransactionCategoryModel> {
        return client
            .select {
                filter {
                    TransactionModel::deviceId eq androidId
                }
            }
            .decodeList<TransactionCategoryModel>()
    }

    private companion object {

        const val TABLE_NAME = "transactionsCategories"
    }
}