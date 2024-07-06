package com.emm.justchill.hh.data.category

import com.emm.justchill.hh.domain.CategoryModel

interface CategorySeeder {

    suspend fun seed(data: List<CategoryModel>)
}