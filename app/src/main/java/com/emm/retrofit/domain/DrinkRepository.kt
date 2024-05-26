package com.emm.retrofit.domain

import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.vo.Result

interface DrinkRepository {

    suspend fun fetchByName(name: String) : Result<List<Drink>>
}