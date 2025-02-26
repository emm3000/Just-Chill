package com.emm.justchill.fake

import com.emm.justchill.experiences.drinks.data.DrinkApiModel
import com.emm.justchill.experiences.drinks.domain.DrinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.UUID

class FakeDrinkRepository : DrinkRepository {

    private val flow = MutableSharedFlow<List<DrinkApiModel>>()

    suspend fun emit() {

        val fakeDrinkApiModel = DrinkApiModel(
            id = UUID.randomUUID().toString(),
            image = UUID.randomUUID().toString(),
            name = UUID.randomUUID().toString(),
            description = UUID.randomUUID().toString()
        )
        val fakeListOfDrinkApiModels: List<DrinkApiModel> = listOf(
            fakeDrinkApiModel
        )

        flow.emit(fakeListOfDrinkApiModels)
    }

    override fun fetchByName(name: String): Flow<List<DrinkApiModel>> {
        return flow { emitAll(flow) }
    }
}