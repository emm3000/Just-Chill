package com.emm.justchill.experiences.hh.domain

import com.emm.justchill.Categories
import com.emm.justchill.core.Result
import com.emm.justchill.core.asResult
import kotlinx.coroutines.flow.Flow

class CategoryLoader(private val repository: CategoryRepository) {

    fun load(): Flow<Result<List<Categories>>> {
        return repository.all().asResult()
    }
}