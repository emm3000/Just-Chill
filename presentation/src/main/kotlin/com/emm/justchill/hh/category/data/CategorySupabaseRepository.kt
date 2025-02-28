package com.emm.justchill.hh.category.data

import com.emm.justchill.hh.shared.TableNames
import com.emm.domain.auth.AuthRepository
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
        return emptyList()
    }

    override suspend fun deleteBy(categoryId: String) {
        client.delete {
            filter {
                CategoryModel::categoryId eq categoryId
            }
        }
    }

    override suspend fun deleteAll() {

    }
}