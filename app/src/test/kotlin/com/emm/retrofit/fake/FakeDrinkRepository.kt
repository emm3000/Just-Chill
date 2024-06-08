package com.emm.retrofit.fake

import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.domain.DrinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.UUID

class FakeDrinkRepository : DrinkRepository {

    private val flow = MutableSharedFlow<List<Drink>>()

    suspend fun emit() {

        val fakeDrink = Drink(
            id = UUID.randomUUID().toString(),
            image = UUID.randomUUID().toString(),
            name = UUID.randomUUID().toString(),
            description = UUID.randomUUID().toString()
        )
        val fakeListOfDrinks: List<Drink> = listOf(
            fakeDrink
        )

        flow.emit(fakeListOfDrinks)
    }

    override fun fetchByName(name: String): Flow<List<Drink>> {
        return flow { emitAll(flow) }
    }
}