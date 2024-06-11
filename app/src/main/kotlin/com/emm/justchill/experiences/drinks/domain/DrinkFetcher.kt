package com.emm.justchill.experiences.drinks.domain

import com.emm.justchill.core.FlowResult
import com.emm.justchill.core.asResult
import com.emm.justchill.experiences.drinks.data.DrinkApiModel

class DrinkFetcher(private val drinkRepository: DrinkRepository) {

    fun fetch(input: String): FlowResult<List<DrinkApiModel>> {
        return drinkRepository.fetchByName(input).asResult()
    }
}