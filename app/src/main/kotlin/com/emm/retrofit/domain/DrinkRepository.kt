package com.emm.retrofit.domain

import com.emm.retrofit.data.model.Drink
import kotlinx.coroutines.flow.Flow

interface DrinkRepository {

    fun fetchByName(name: String) : Flow<List<Drink>>
}