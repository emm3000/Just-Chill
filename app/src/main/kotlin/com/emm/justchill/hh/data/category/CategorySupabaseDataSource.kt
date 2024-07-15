package com.emm.justchill.hh.data.category

import com.emm.justchill.hh.domain.CategoryModel
import com.emm.justchill.hh.domain.AndroidDataProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

class CategorySupabaseDataSource(
    supabaseClient: SupabaseClient,
    androidDataProvider: AndroidDataProvider,
) : CategoryNetworkDataSource {

    private val androidId: String = androidDataProvider.androidId()
    private val builder: PostgrestQueryBuilder = supabaseClient.from(TABLE_NAME)

    override suspend fun upsert(category: CategoryModel) {
        builder.upsert(category)
    }

    override suspend fun upsert(categories: List<CategoryModel>) {
        builder.upsert(categories)
    }

    override suspend fun retrieve(): List<CategoryModel> = builder
        .select {
            filter { CategoryModel::deviceId eq androidId }
        }
        .decodeList<CategoryModel>()

    private companion object {

        const val TABLE_NAME = "categories"
    }
}