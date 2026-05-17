package com.emm.data.category

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeDbCall
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.CategoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class DefaultCategoryRepository(
    private val localDataSource: CategoryLocalDataSource,
) : CategoryRepository {

    override fun all(): Flow<List<Category>> = localDataSource.all().catchAsDomainException()

    override suspend fun find(categoryId: CategoryId): Category? = safeDbCall {
        localDataSource.find(categoryId.value).firstOrNull()
    }

    override suspend fun create(categoryUpsert: CategoryUpsert): Unit = safeDbCall {
        localDataSource.create(categoryUpsert)
        Unit
    }

    override suspend fun update(categoryId: CategoryId, categoryUpsert: CategoryUpsert): Unit = safeDbCall {
        localDataSource.update(categoryId.value, categoryUpsert)
        Unit
    }

    override suspend fun delete(categoryId: CategoryId): Unit = safeDbCall {
        localDataSource.delete(categoryId.value)
        Unit
    }

    override suspend fun count(): Long = safeDbCall {
        localDataSource.countDefaults()
    }
}
