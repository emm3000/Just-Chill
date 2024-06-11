package com.emm.justchill.experiences.drinks.data

import com.emm.justchill.experiences.drinks.domain.DrinkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

class DefaultDrinkRepository(
    private val networkDataSource: DrinkFetcher,
    private val diskDataSource: DrinkFetcher,
    private val diskSaver: DrinkSaver,
) : DrinkRepository {

    override fun fetchByName(name: String): Flow<List<DrinkApiModel>> {
        return tryFetchLocally(name)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun tryFetchLocally(name: String): Flow<List<DrinkApiModel>> {
        return diskDataSource.fetchByName(name)
            .flatMapConcat { localDrinks ->
                validateIfExistDrinksInDisk(localDrinks, name)
            }
    }

    private fun validateIfExistDrinksInDisk(
        localDrinks: List<DrinkApiModel>,
        name: String,
    ): Flow<List<DrinkApiModel>> {
        return if (localDrinks.isEmpty()) {
            networkDataSource.fetchByName(name)
                .onEach { diskSaver.saveDrinks(name, it) }
        } else {
            flowOf(localDrinks)
        }
    }
}