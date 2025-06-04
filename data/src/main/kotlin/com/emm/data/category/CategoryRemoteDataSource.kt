package com.emm.data.category

import com.emm.data.auth.UserIdProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryRemoteDataSource(
    userIdProvider: UserIdProvider,
    client: SupabaseClient,
): UserIdProvider by userIdProvider {

    private val table: PostgrestQueryBuilder = client.from("categories_v2")

    suspend fun upsert(categories: List<CategoryModel>) = withContext(Dispatchers.IO) {
        val categoryModels = categories.map(::attachUserIdToCategory)
        table.upsert(categoryModels)
    }

    private fun attachUserIdToCategory(
        categoryModel: CategoryModel,
    ): CategoryModel = categoryModel.copy(userId = userId)
}