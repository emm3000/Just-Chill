package com.emm.retrofit.experiences.drinks.domain

import com.emm.retrofit.core.FlowResult
import com.emm.retrofit.core.asResult
import com.emm.retrofit.experiences.drinks.data.DrinkApiModel

class DrinkFetcher(private val drinkRepository: DrinkRepository) {

    fun fetch(input: String): FlowResult<List<DrinkApiModel>> {
        return drinkRepository.fetchByName(input).asResult()
    }
}