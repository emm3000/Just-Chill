package com.emm.justchill.hh.category.data

import com.emm.justchill.hh.shared.TableNames
import com.emm.justchill.hh.auth.domain.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class CategorySupabaseRepository(
    supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
): CategoryRemoteRepository {

    private val client: PostgrestQueryBuilder by lazy {
        supabaseClient.from(TableNames.CATEGORY_TABLE)
    }

    override suspend fun upsert(category: CategoryModel) {
        client.upsert(category)
    }

    override suspend fun retrieve(): List<CategoryModel> {
        val userId: String = authRepository.session()?.id ?: return emptyList()
        return client
            .select {
                filter { CategoryModel::userId eq userId }
            }
            .decodeList<CategoryModel>()
    }

    override suspend fun deleteBy(categoryId: String) {
        client.delete {
            filter {
                CategoryModel::categoryId eq categoryId
            }
        }
    }

    override suspend fun deleteAll() {
        val userId: String = authRepository.session()?.id ?: return
        client.delete {
            filter {
                CategoryModel::userId eq userId
            }
        }
    }
}