package com.emm.retrofit.domain

import com.emm.retrofit.data.DataSource
import com.emm.retrofit.data.model.Drink
import kotlinx.coroutines.flow.Flow

class DrinkRepositoryImpl(private val dataSource: DataSource): DrinkRepository {

    override fun fetchByName(name: String): Flow<List<Drink>> {
        return dataSource.fetchDrinkByName(name)
    }
}