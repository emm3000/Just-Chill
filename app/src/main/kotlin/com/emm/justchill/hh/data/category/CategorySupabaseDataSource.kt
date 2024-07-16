package com.emm.justchill.hh.data.category

import com.emm.justchill.hh.data.TableNames
import com.emm.justchill.hh.domain.CategoryModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class CategorySupabaseDataSource(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
) : CategoryNetworkDataSource {

    private val builder: PostgrestQueryBuilder = supabaseClient.from(TableNames.CATEGORY_TABLE)

    override suspend fun upsert(category: CategoryModel) {
        supabaseClient.auth.refreshCurrentSession()
        builder.upsert(category)
    }

    override suspend fun upsert(categories: List<CategoryModel>) {
        builder.upsert(categories)
    }

    override suspend fun retrieve(): List<CategoryModel> {
        val userId: String = authRepository.session()?.id ?: return emptyList()
        return builder
            .select {
                filter { CategoryModel::userId eq userId }
            }
            .decodeList<CategoryModel>()
    }
}