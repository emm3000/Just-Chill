package com.emm.retrofit.domain

import com.emm.retrofit.data.DataSource
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.vo.Result

class DrinkRepositoryImpl(private val dataSource: DataSource): DrinkRepository {

    override suspend fun fetchByName(name: String): Result<List<Drink>> {
        return dataSource.fetchDrinkByName(name)
    }
}