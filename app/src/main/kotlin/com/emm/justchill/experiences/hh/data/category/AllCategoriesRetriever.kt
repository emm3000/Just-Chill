package com.emm.justchill.experiences.hh.data.category

import com.emm.justchill.Categories
import kotlinx.coroutines.flow.Flow

interface AllCategoriesRetriever {

    fun retrieve(): Flow<List<Categories>>
}