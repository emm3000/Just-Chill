package com.emm.justchill.experiences.hh.data

import com.emm.justchill.Categories
import com.emm.justchill.experiences.hh.domain.CategoryRepository
import kotlinx.coroutines.flow.Flow

class DefaultCategoryRepository(
    private val categorySaver: CategorySaver,
    private val retriever: AllItemsRetriever<Categories>,
) : CategoryRepository {

    override suspend fun add(name: String, type: String) {
        categorySaver.save(name, type)
    }

    override fun all(): Flow<List<Categories>> {
        return retriever.retrieve()
    }
}