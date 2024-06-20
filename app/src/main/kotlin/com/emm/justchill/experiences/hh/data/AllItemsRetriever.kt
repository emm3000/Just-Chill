package com.emm.justchill.experiences.hh.data

import kotlinx.coroutines.flow.Flow

interface AllItemsRetriever<T> {

    fun retrieve(): Flow<List<T>>
}