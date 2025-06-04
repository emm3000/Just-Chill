package com.emm.data.category

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteCategoryDataSource(client: SupabaseClient) {

    private val table: PostgrestQueryBuilder = client.from("categories_v2")

    private val userId: String = client.auth.currentUserOrNull()?.id ?: throw IllegalStateException()

    suspend fun upsert(categories: List<CategoryModel>) = withContext(Dispatchers.IO) {
        val categoryModels = categories.map(::attachUserIdToCategory)
        table.upsert(categoryModels)
    }

    private fun attachUserIdToCategory(categoryModel: CategoryModel) = categoryModel.copy(userId = userId)
}