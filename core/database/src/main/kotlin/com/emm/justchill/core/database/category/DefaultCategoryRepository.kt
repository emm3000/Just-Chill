package com.emm.justchill.core.database.category

import com.emm.justchill.core.database.shared.catchAsDomainException
import com.emm.justchill.core.database.shared.safeDbCall
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.category.CategoryUpsert
import com.emm.justchill.core.domain.shared.CategoryId
import kotlinx.coroutines.flow.Flow

class DefaultCategoryRepository(private val localDataSource: CategoryLocalDataSource) : CategoryRepository {

    override fun all(): Flow<List<Category>> = localDataSource.all().catchAsDomainException()

    override suspend fun create(categoryUpsert: CategoryUpsert): Unit = safeDbCall {
        localDataSource.create(categoryUpsert)
        Unit
    }

    override suspend fun update(categoryId: CategoryId, categoryUpsert: CategoryUpsert): Unit = safeDbCall {
        localDataSource.update(categoryId.value, categoryUpsert)
        Unit
    }

    override suspend fun delete(categoryId: CategoryId): Unit = safeDbCall {
        localDataSource.softDelete(categoryId.value)
        Unit
    }

    override suspend fun count(): Long = safeDbCall {
        localDataSource.countDefaults()
    }
}
