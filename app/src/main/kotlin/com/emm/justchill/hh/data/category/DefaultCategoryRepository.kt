package com.emm.justchill.hh.data.category

import com.emm.justchill.Categories
import com.emm.justchill.hh.domain.CategoryModel
import com.emm.justchill.hh.domain.category.CategoryRepository
import kotlinx.coroutines.flow.Flow

class DefaultCategoryRepository(
    private val categorySaver: CategorySaver,
    private val retriever: AllCategoriesRetriever,
    private val categorySeeder: CategorySeeder,
) : CategoryRepository {

    override suspend fun add(name: String, type: String) {
        categorySaver.save(name, type)
    }

    override fun all(): Flow<List<Categories>> {
        return retriever.retrieve()
    }

    override suspend fun seed(data: List<CategoryModel>) {
        categorySeeder.seed(data)
    }
}