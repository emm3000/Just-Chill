package com.emm.justchill.hh.data.transactioncategory

import com.emm.justchill.hh.data.TableNames
import com.emm.justchill.hh.domain.TransactionCategoryModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class TransactionCategorySupabaseDataSource(
    supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
) : TransactionCategoryNetworkDataSource {

    private val client: PostgrestQueryBuilder by lazy {
        supabaseClient.from(TableNames.TRANSACTION_CATEGORY_TABLE)
    }

    override suspend fun upsert(transactionCategory: TransactionCategoryModel) {
        client.upsert(transactionCategory)
    }

    override suspend fun upsert(transactionsCategories: List<TransactionCategoryModel>) {
        client.upsert(transactionsCategories)
    }

    override suspend fun retrieve(): List<TransactionCategoryModel> {
        val userId: String = authRepository.session()?.id ?: return emptyList()
        return client
            .select {
                filter {
                    TransactionCategoryModel::userId eq userId
                }
            }
            .decodeList<TransactionCategoryModel>()
    }
}